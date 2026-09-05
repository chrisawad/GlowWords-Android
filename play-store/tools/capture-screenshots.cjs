const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { WebSocket } = require('undici');

const PROJECT = path.resolve(__dirname, '..', '..');
const OUTPUT = path.join(PROJECT, 'play-store', 'screenshots', 'phone');
const URL = process.argv[2] || 'https://glow.chrisawad.com';
const DEBUG_PORT = 9223;
const CSS_WIDTH = 360;
const CSS_HEIGHT = 640;
const DEVICE_SCALE_FACTOR = 3;

function delay(milliseconds) {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function waitForJson(url, timeoutMs = 30_000) {
    const deadline = Date.now() + timeoutMs;
    let lastError;
    while (Date.now() < deadline) {
        try {
            const response = await fetch(url);
            if (response.ok) return response.json();
        } catch (error) {
            lastError = error;
        }
        await delay(200);
    }
    throw lastError || new Error(`Timed out waiting for ${url}`);
}

class CdpClient {
    constructor(url) {
        this.nextId = 1;
        this.pending = new Map();
        this.listeners = new Map();
        this.socket = new WebSocket(url);
        this.opened = new Promise((resolve, reject) => {
            this.socket.addEventListener('open', resolve, { once: true });
            this.socket.addEventListener('error', reject, { once: true });
        });
        this.socket.addEventListener('message', (event) => this.onMessage(event));
    }

    onMessage(event) {
        const message = JSON.parse(String(event.data));
        if (message.id) {
            const pending = this.pending.get(message.id);
            if (!pending) return;
            this.pending.delete(message.id);
            if (message.error) pending.reject(new Error(message.error.message));
            else pending.resolve(message.result);
            return;
        }

        const listeners = this.listeners.get(message.method) || [];
        this.listeners.delete(message.method);
        listeners.forEach((listener) => listener.resolve(message.params));
    }

    async send(method, params = {}) {
        await this.opened;
        const id = this.nextId++;
        const response = new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
        this.socket.send(JSON.stringify({ id, method, params }));
        return response;
    }

    waitForEvent(method, timeoutMs = 30_000) {
        return new Promise((resolve, reject) => {
            const listener = { resolve, reject };
            this.listeners.set(method, [...(this.listeners.get(method) || []), listener]);
            setTimeout(() => {
                const listeners = this.listeners.get(method) || [];
                this.listeners.set(method, listeners.filter((item) => item !== listener));
                reject(new Error(`Timed out waiting for ${method}`));
            }, timeoutMs).unref();
        });
    }

    close() {
        this.socket.close();
    }
}

async function evaluate(client, expression) {
    const result = await client.send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true,
    });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.text || 'Browser evaluation failed');
    return result.result.value;
}

async function waitForSelector(client, selector, timeoutMs = 30_000) {
    const encoded = JSON.stringify(selector);
    await evaluate(client, `(async () => {
        const deadline = Date.now() + ${timeoutMs};
        while (Date.now() < deadline) {
            if (document.querySelector(${encoded})) return true;
            await new Promise((resolve) => setTimeout(resolve, 100));
        }
        throw new Error('Timed out waiting for ' + ${encoded});
    })()`);
}

async function settlePage(client) {
    await evaluate(client, `(async () => {
        if (document.fonts?.ready) await document.fonts.ready;
        await Promise.all([...document.images].map((image) => image.complete
            ? Promise.resolve()
            : new Promise((resolve) => {
                image.addEventListener('load', resolve, { once: true });
                image.addEventListener('error', resolve, { once: true });
            })));
        await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    })()`);
    await delay(300);
}

async function capture(client, filename) {
    await settlePage(client);
    const result = await client.send('Page.captureScreenshot', {
        format: 'png',
        fromSurface: true,
        captureBeyondViewport: false,
    });
    fs.writeFileSync(path.join(OUTPUT, filename), Buffer.from(result.data, 'base64'));
    process.stdout.write(`Captured ${filename}\n`);
}

async function readSolutions(client) {
    return evaluate(client, `(() => {
        const cells = [...document.querySelectorAll('[data-cell]')];
        const size = Math.sqrt(cells.length);
        const grid = Array.from({ length: size }, () => Array(size));
        cells.forEach((cell) => {
            grid[Number(cell.dataset.row)][Number(cell.dataset.col)] = cell.textContent.trim();
        });
        const words = [...document.querySelectorAll('.word-chips button')]
            .map((button) => button.textContent.replace(/[^A-Z]/g, ''));
        const directions = [-1, 0, 1].flatMap((row) => [-1, 0, 1].map((col) => [row, col]))
            .filter(([row, col]) => row || col);
        const solutions = [];

        for (const word of words) {
            let found = null;
            for (let row = 0; row < size && !found; row += 1) {
                for (let col = 0; col < size && !found; col += 1) {
                    for (const [rowStep, colStep] of directions) {
                        const endRow = row + rowStep * (word.length - 1);
                        const endCol = col + colStep * (word.length - 1);
                        if (endRow < 0 || endRow >= size || endCol < 0 || endCol >= size) continue;
                        const letters = Array.from({ length: word.length }, (_, index) =>
                            grid[row + rowStep * index][col + colStep * index]).join('');
                        if (letters === word) {
                            const start = document.querySelector('[data-row="' + row + '"][data-col="' + col + '"]').getBoundingClientRect();
                            const end = document.querySelector('[data-row="' + endRow + '"][data-col="' + endCol + '"]').getBoundingClientRect();
                            found = {
                                word,
                                start: { x: start.left + start.width / 2, y: start.top + start.height / 2 },
                                end: { x: end.left + end.width / 2, y: end.top + end.height / 2 },
                            };
                            break;
                        }
                    }
                }
            }
            if (!found) throw new Error('Could not locate ' + word + ' in the board');
            solutions.push(found);
        }
        return solutions;
    })()`);
}

async function dragWord(client, solution) {
    await client.send('Input.dispatchMouseEvent', {
        type: 'mousePressed',
        x: solution.start.x,
        y: solution.start.y,
        button: 'left',
        buttons: 1,
        clickCount: 1,
    });
    for (let step = 1; step <= 16; step += 1) {
        const progress = step / 16;
        await client.send('Input.dispatchMouseEvent', {
            type: 'mouseMoved',
            x: solution.start.x + (solution.end.x - solution.start.x) * progress,
            y: solution.start.y + (solution.end.y - solution.start.y) * progress,
            button: 'left',
            buttons: 1,
        });
    }
    await client.send('Input.dispatchMouseEvent', {
        type: 'mouseReleased',
        x: solution.end.x,
        y: solution.end.y,
        button: 'left',
        buttons: 0,
        clickCount: 1,
    });
    await delay(180);
}

async function main() {
    fs.mkdirSync(OUTPUT, { recursive: true });
    const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'glowwords-chromium-'));
    const chrome = spawn('chromium', [
        '--headless=new',
        '--no-sandbox',
        '--disable-dev-shm-usage',
        '--disable-background-networking',
        '--hide-scrollbars',
        `--remote-debugging-port=${DEBUG_PORT}`,
        `--user-data-dir=${profile}`,
        'about:blank',
    ], { stdio: 'ignore' });

    let client;
    try {
        const targets = await waitForJson(`http://127.0.0.1:${DEBUG_PORT}/json/list`);
        const page = targets.find((target) => target.type === 'page');
        if (!page) throw new Error('Chromium did not expose a page target');
        client = new CdpClient(page.webSocketDebuggerUrl);

        await client.send('Page.enable');
        await client.send('Runtime.enable');
        await client.send('Emulation.setDeviceMetricsOverride', {
            width: CSS_WIDTH,
            height: CSS_HEIGHT,
            deviceScaleFactor: DEVICE_SCALE_FACTOR,
            mobile: true,
            screenWidth: CSS_WIDTH,
            screenHeight: CSS_HEIGHT,
        });
        await client.send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 });
        await client.send('Emulation.setEmulatedMedia', {
            features: [{ name: 'prefers-reduced-motion', value: 'reduce' }],
        });
        await client.send('Network.setUserAgentOverride', {
            userAgent: 'Mozilla/5.0 (Linux; Android 16; Pixel 10 Pro XL Build/BP2A) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/151.0.0.0 Mobile Safari/537.36 wv',
            platform: 'Android',
        });

        const loaded = client.waitForEvent('Page.loadEventFired');
        await client.send('Page.navigate', { url: URL });
        await loaded;
        await waitForSelector(client, '.setup-shell');
        await evaluate(client, 'window.scrollTo(0, 0)');
        await capture(client, '05-welcome.png');

        await evaluate(client, `document.querySelector('.setup-card').scrollIntoView({ block: 'start' })`);
        await capture(client, '06-choose-challenge.png');

        await evaluate(client, `document.querySelector('.start-button').click()`);
        await waitForSelector(client, '.letter-board');
        await capture(client, '01-game-board.png');

        const solutions = await readSolutions(client);
        for (const solution of solutions.slice(0, 2)) await dragWord(client, solution);
        await capture(client, '02-words-found.png');

        await evaluate(client, `document.querySelector('.word-chips button:not(.found-word)').click()`);
        await waitForSelector(client, '.practice-card');
        await capture(client, '03-word-practice.png');
        await evaluate(client, `document.querySelector('.practice-close').click()`);
        await waitForSelector(client, '.letter-board');

        for (const solution of solutions.slice(2)) await dragWord(client, solution);
        await waitForSelector(client, '.result-card');
        await capture(client, '04-victory.png');
    } finally {
        client?.close();
        chrome.kill('SIGTERM');
    }
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});

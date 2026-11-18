// Starts one arm's MCP server the way claude would (arms.json), opens a page through it and takes a
// snapshot: proves the server and its browser work in the image, and prints the sizes an agent pays.
//
//   node smoke.mjs <arms.json> <arm> <url>
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';

const [armsFile, arm, url] = process.argv.slice(2);
const server = JSON.parse(readFileSync(armsFile, 'utf8')).mcpServers[arm];
if (!server) throw new Error(`no arm ${arm} in ${armsFile}`);
const tools = {
  'unified-test-mcp': ['navigate', { url }, 'snapshot'],
  'playwright': ['browser_navigate', { url }, 'browser_snapshot'],
  'chrome-devtools': ['navigate_page', { type: 'url', url }, 'take_snapshot'],
}[arm];

const child = spawn(server.command, server.args, { stdio: ['pipe', 'pipe', 'inherit'] });
let buffer = '';
const pending = new Map();
child.stdout.on('data', chunk => {
  buffer += chunk;
  let newline;
  while ((newline = buffer.indexOf('\n')) >= 0) {
    const line = buffer.slice(0, newline).trim();
    buffer = buffer.slice(newline + 1);
    if (!line) continue;
    const message = JSON.parse(line);
    pending.get(message.id)?.(message);
  }
});
let nextId = 1;
function request(method, params) {
  const id = nextId++;
  child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
  return new Promise(resolve => pending.set(id, resolve));
}
const text = result => (result.result?.content ?? []).map(c => c.text ?? `[${c.type}]`).join('\n');

const started = Date.now();
await request('initialize', { protocolVersion: '2025-06-18', capabilities: {}, clientInfo: { name: 'smoke', version: '1' } });
child.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) + '\n');
const list = await request('tools/list', {});
console.log(`${arm}: ${list.result.tools.length} tools, tools/list ${JSON.stringify(list.result.tools).length} chars`);
// chrome-devtools-mcp targets a page by its id, from list_pages
let page = {};
if (arm === 'chrome-devtools') {
  const pages = text(await request('tools/call', { name: 'list_pages', arguments: {} }));
  page = { pageId: Number(pages.match(/(\d+):/)?.[1] ?? 1) };
}
const navigated = await request('tools/call', { name: tools[0], arguments: { ...tools[1], ...page } });
console.log(`${tools[0]}: ${navigated.result?.isError ? 'ERROR ' : ''}${text(navigated).length} chars`);
const snapshot = await request('tools/call', { name: tools[2], arguments: page });
const shown = text(snapshot);
console.log(`${tools[2]}: ${snapshot.result?.isError ? 'ERROR ' : ''}${shown.length} chars, ${Date.now() - started} ms in all`);
console.log(shown.split('\n').slice(0, 25).join('\n'));
child.kill();
process.exit(0);

const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const test = require('node:test');

const source = fs.readFileSync('app/src/main/assets/tv_navigation.js', 'utf8');

function element(x, y, tagName = 'BUTTON') {
  const classes = new Set();
  return {
    tagName,
    disabled: false,
    isConnected: true,
    classList: { add: value => classes.add(value), remove: value => classes.delete(value), contains: value => classes.has(value) },
    getBoundingClientRect: () => ({ left: x - 20, top: y - 20, width: 40, height: 40 }),
    focus() {},
    scrollIntoView() {}
  };
}

function navigator(elements) {
  const scrolls = [];
  const document = {
    activeElement: null,
    body: {},
    head: { appendChild() {} },
    getElementById: () => null,
    createElement: () => ({}),
    querySelectorAll: () => elements
  };
  const window = { scrollBy: (...args) => scrolls.push(args) };
  const context = { window, document, innerWidth: 400, innerHeight: 300, getComputedStyle: () => ({ display: 'block', visibility: 'visible', pointerEvents: 'auto' }) };
  vm.runInNewContext(source, context);
  return { tv: window.VelaTV, scrolls };
}

test('D-pad moves focus spatially and OK returns touch coordinates', () => {
  const top = element(200, 40);
  const right = element(320, 150);
  const { tv } = navigator([top, right]);
  assert.equal(tv.move('right'), true);
  assert.equal(right.classList.contains('vela-tv-focused'), true);
  assert.deepEqual(Array.from(tv.activate()), [0.8, 0.5, false]);
  assert.equal(tv.move('up'), true);
  assert.equal(top.classList.contains('vela-tv-focused'), true);
});

test('iframe activation requests pointer mode and absent targets scroll', () => {
  const frame = element(320, 150, 'IFRAME');
  const { tv } = navigator([frame]);
  assert.equal(tv.move('right'), true);
  assert.deepEqual(Array.from(tv.activate()), [0.8, 0.5, true]);
  const empty = navigator([]);
  assert.equal(empty.tv.move('down'), false);
  assert.deepEqual(empty.scrolls[0], [0, 195]);
});

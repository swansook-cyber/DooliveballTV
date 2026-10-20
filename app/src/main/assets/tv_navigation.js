(function () {
  if (window.VelaTV) return;

  const selector = 'a[href],button,input,select,textarea,video,iframe,[role="button"],[role="link"],[tabindex],[onclick],[contenteditable="true"],[style*="cursor: pointer"],[style*="cursor:pointer"]';
  let selected = null;

  function targets() {
    return Array.from(document.querySelectorAll(selector)).filter(function (element) {
      const rect = element.getBoundingClientRect();
      const style = getComputedStyle(element);
      return rect.width > 8 && rect.height > 8 &&
        style.display !== 'none' && style.visibility !== 'hidden' &&
        style.pointerEvents !== 'none' && !element.disabled;
    });
  }

  function center(element) {
    const rect = element.getBoundingClientRect();
    return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
  }

  function showFocus(element) {
    if (!document.getElementById('vela-tv-focus-style')) {
      const style = document.createElement('style');
      style.id = 'vela-tv-focus-style';
      style.textContent = '.vela-tv-focused { outline: 4px solid #ff1694 !important; outline-offset: 3px !important; }';
      (document.head || document.documentElement).appendChild(style);
    }
    if (selected) selected.classList.remove('vela-tv-focused');
    selected = element;
    selected.classList.add('vela-tv-focused');
    if (typeof selected.focus === 'function') selected.focus({ preventScroll: true });
    selected.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  }

  window.VelaTV = {
    move: function (direction) {
      const elements = targets();
      if (!elements.length) {
        window.scrollBy(0, (direction === 'up' ? -1 : 1) * innerHeight * 0.65);
        return false;
      }
      const current = elements.includes(selected) ? selected : null;
      const origin = current ? center(current) : { x: innerWidth / 2, y: innerHeight / 2 };
      let best = null;
      let bestScore = Infinity;
      for (const element of elements) {
        if (element === current) continue;
        const point = center(element);
        const dx = point.x - origin.x;
        const dy = point.y - origin.y;
        const forward = direction === 'right' ? dx : direction === 'left' ? -dx : direction === 'down' ? dy : -dy;
        if (forward <= 2) continue;
        const sideways = direction === 'right' || direction === 'left' ? Math.abs(dy) : Math.abs(dx);
        const score = forward + sideways * 2 + (point.x < 0 || point.x > innerWidth || point.y < 0 || point.y > innerHeight ? innerHeight : 0);
        if (score < bestScore) { best = element; bestScore = score; }
      }
      if (best) { showFocus(best); return true; }
      window.scrollBy(0, (direction === 'up' ? -1 : direction === 'down' ? 1 : 0) * innerHeight * 0.65);
      return false;
    },
    activate: function () {
      const element = selected && selected.isConnected ? selected : document.activeElement;
      if (!element || element === document.body) return null;
      const point = center(element);
      return [point.x / innerWidth, point.y / innerHeight, element.tagName === 'IFRAME'];
    }
  };
})();

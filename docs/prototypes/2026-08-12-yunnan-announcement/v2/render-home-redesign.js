const { chromium } = require('playwright');
const path = require('path');
const { pathToFileURL } = require('url');

(async () => {
  const root = __dirname;
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  });
  const page = await browser.newPage({ viewport: { width: 1180, height: 2500 }, deviceScaleFactor: 1 });
  await page.goto(pathToFileURL(path.join(root, 'home-redesign-prototype.html')).href, { waitUntil: 'load' });
  await page.evaluate(async () => {
    await document.fonts.ready;
    await Promise.all([...document.images].map((image) => image.complete ? Promise.resolve() : new Promise((resolve) => {
      image.addEventListener('load', resolve, { once: true });
      image.addEventListener('error', resolve, { once: true });
    })));
  });
  for (const [selector, filename] of [
    ['#home-with-payout', '01-home-redesign-with-payout.png'],
    ['#home-without-payout', '02-home-redesign-without-payout.png'],
  ]) {
    await page.locator(selector).screenshot({ path: path.join(root, filename) });
  }
  await browser.close();
})();

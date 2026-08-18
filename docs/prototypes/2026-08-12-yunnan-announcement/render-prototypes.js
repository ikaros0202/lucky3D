const { chromium } = require('playwright');
const path = require('path');
const { pathToFileURL } = require('url');

(async () => {
  const root = __dirname;
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  });
  const page = await browser.newPage({
    viewport: { width: 1180, height: 2500 },
    deviceScaleFactor: 1,
  });
  await page.goto(pathToFileURL(path.join(root, 'lucky3d-yunnan-prototypes.html')).href, {
    waitUntil: 'load',
  });
  await page.evaluate(async () => {
    await document.fonts.ready;
    const images = [...document.images];
    await Promise.all(images.map((image) => image.complete
      ? Promise.resolve()
      : new Promise((resolve) => {
          image.addEventListener('load', resolve, { once: true });
          image.addEventListener('error', resolve, { once: true });
        })));
  });

  const outputs = [
    ['#home-payout', '01-home-yunnan-payout.png'],
    ['#home-no-payout', '02-home-yunnan-no-payout.png'],
    ['#issue-detail', '03-issue-detail-yunnan.png'],
    ['#issue-selector', '04-issue-selector-open.png'],
  ];

  for (const [selector, filename] of outputs) {
    await page.locator(selector).screenshot({ path: path.join(root, filename) });
  }
  await browser.close();
})();

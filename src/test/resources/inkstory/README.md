# InkStory migration fixtures

Captured from public, unauthenticated responses on 2026-09-13, after the
[12 September site rewrite](https://inkstory.net/blog/2026-09-12-obnovlenie-sajta).
Only the `$tsr-stream-barrier` state script is retained from each HTML response.

- `details.html`: https://inkstory.net/content/chainsaw_man-p2
- `chapters.html`: https://inkstory.net/content/chainsaw_man-p2/chapters
- `chapter.html`: https://inkstory.net/content/chainsaw_man-p2/a23602d4-a643-46e3-afed-e723c65f434e

The title has 141 chapters; the selected chapter advertises 21 image pages.
Tests execute the production state extraction script in Node and exercise both
OVH source variants. A missing-poster variant also simulates Android WebView's
quoted string result. These fixtures do not verify Android WebView execution or
the device image decoder.

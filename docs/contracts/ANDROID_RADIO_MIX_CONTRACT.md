# Android Artist Radio and Discovery Mix (LB-20)

Server authority: [explicit radio mix actions](https://github.com/abucalip/navi-librarian/blob/main/docs/contracts/RADIO_MIX_ACTIONS_CONTRACT.md).

- Every artist layout offers Start Artist Radio and Discover a mix when an artist MBID is available. Discover also offers a tag-seeded mix.
- Artist Radio selects Familiar, Explore or Deep cuts; Discovery Mix selects Easy, Medium or Hard. Generation explicitly posts `kind`, exactly one `artist_mbid` or `tag`, and the selected controls to `/api/radio-mixes/preview`.
- Only a ready, stored Artist Radio preview with playable owned tracks starts native playback. Discovery previews never start playback automatically. Empty, unconfigured, unavailable, failed and cancelled requests preserve the existing queue.
- Detail reads `GET /api/radio-mixes/{preview_id}` live. Play owned tracks rechecks this preview before replacing the queue, preserving source order and repeated occurrences. Unknown ownership and invalid song IDs are never playable.
- Discovery downloads post only the stored preview ID and original track position to `/api/radio-mixes/{preview_id}/tracks/{position}/download`. Metadata resolution, identity refusal and deduplication remain server-side; Artist Radio never exposes downloads.
- Detail reuses Inbox acquisition status, progress, retry and lifecycle-scoped refresh. Imported tracks require confirmed indexed ownership before playback; Refresh status is available manually. Preview generation and reads never use offline cached results.
- These mixes are separate from live internet Radio, Playlist Inbox subscriptions and Navidrome playlist saving. LB-20 adds native playback and discovery downloads only.

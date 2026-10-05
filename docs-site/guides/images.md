# Images

Badges, logos, kits, photos, posters and banners are image URLs in fields such as `strBadge`, `strLogo`, `strThumb`, `strCutout`, `strHomeTeamBadge` and `strPoster`.

## Smaller copies

Add `/medium`, `/small` or `/tiny` to an image URL for a smaller copy:

```
https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png         128 KB
https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png/medium   92 KB
https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png/small    46 KB
https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png/tiny     14 KB
```

This works for images on `r2.thesportsdb.com` and under `www.thesportsdb.com/images/media/`. It **doesn't** work for other `www.thesportsdb.com` images, such as the sport pictures under `/images/sports/` (they return 404). Use `/tiny` or `/small` for lists and cards.

## Using artwork in an app

Check the [terms of use](https://www.thesportsdb.com/docs_terms_of_use.php) before publishing. Records with photos carry two fields for this:

- `strCreativeCommons`: `Yes` if the artwork is Creative Commons. Artwork marked `No`, or with no value, may not be used in published apps.
- `strCreativeCommonsAttribution`: the credit line to show with the image.

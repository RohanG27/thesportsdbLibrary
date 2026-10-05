<?php

declare(strict_types=1);

namespace SportsDb\Model;

use SportsDb\Internal\Rec;

/** Web and social links. TheSportsDB often gives these without a scheme (www.facebook.com/Arsenal). */
final readonly class Socials
{
    /** @internal */
    public function __construct(
        public ?string $website, // strWebsite
        public ?string $facebook, // strFacebook
        public ?string $twitter, // strTwitter
        public ?string $instagram, // strInstagram
        public ?string $youtube, // strYoutube
        public ?string $rss, // strRSS
    ) {
    }

    /** @internal */
    public static function fromRec(Rec $r): self
    {
        return new self($r->s('strWebsite'), $r->s('strFacebook'), $r->s('strTwitter'), $r->s('strInstagram'),
            $r->s('strYoutube'), $r->s('strRSS'));
    }
}

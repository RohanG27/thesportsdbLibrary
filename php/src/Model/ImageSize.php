<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Model;

/** Image sizes TheSportsDB serves by appending a path suffix. */
enum ImageSize: string
{
    /** About 70% of the original's bytes. */
    case Medium = '/medium';
    /** About 35%; good for cards. */
    case Small = '/small';
    /** About 10%; good for lists. */
    case Tiny = '/tiny';

    /**
     * The same image at this size. Only r2.thesportsdb.com and www.thesportsdb.com/images/media/
     * images support the suffixes (others return 404, verified 5 Oct 2026); other URLs and null
     * are returned unchanged.
     */
    public function of(?string $url): ?string
    {
        if ($url === null || !(str_starts_with($url, 'https://r2.thesportsdb.com/')
            || str_starts_with($url, 'https://www.thesportsdb.com/images/media/'))) {
            return $url;
        }
        foreach (self::cases() as $size) {
            if (str_ends_with($url, $size->value)) {
                $url = substr($url, 0, -\strlen($size->value));
            }
        }
        return $url . $this->value;
    }
}

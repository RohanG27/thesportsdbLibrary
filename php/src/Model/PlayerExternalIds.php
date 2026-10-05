<?php

declare(strict_types=1);

namespace SportsDb\Model;

/** Ids of the same player in other databases. */
final readonly class PlayerExternalIds
{
    /** @internal */
    public function __construct(
        public ?int $apiFootball, // idAPIfootball
        public ?string $espn, // idESPN
        public ?string $google, // idGoogle, e.g. /g/11cpprmr81
        public ?string $transfermarkt, // idTransferMkt
        public ?string $wikidata, // idWikidata, e.g. Q9144353
        public ?string $soccerXmlTeam, // intSoccerXMLTeamID
    ) {
    }
}

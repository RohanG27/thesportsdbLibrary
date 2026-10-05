<?php

declare(strict_types=1);

namespace RohanG27\TheSportsDb\Tests;

use PHPUnit\Framework\TestCase;

final class GeneratedTest extends TestCase
{
    public function testModelsAreGeneratedFromTheKotlinModels(): void
    {
        exec('python3 ' . escapeshellarg(\dirname(__DIR__) . '/tools/gen_models.py') . ' --check 2>&1', $output, $code);
        self::assertSame(0, $code, implode("\n", $output));
    }
}

<?php

// Copies the recorded API responses from the Kotlin project (../kotlin/src/test/resources/fixtures, recorded by
// ../tools/record-fixtures.sh) into tests/fixtures, so every library tests against the same responses.

declare(strict_types=1);

$source = dirname(__DIR__, 2) . '/kotlin/src/test/resources/fixtures';
$target = dirname(__DIR__) . '/tests/fixtures';

$remove = static function (string $dir) use (&$remove): void {
    foreach (glob("$dir/*") ?: [] as $path) {
        is_dir($path) ? $remove($path) : unlink($path);
    }
    if (is_dir($dir)) {
        rmdir($dir);
    }
};
$remove($target);
$count = 0;
foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($source, FilesystemIterator::SKIP_DOTS)) as $file) {
    $relative = substr($file->getPathname(), strlen($source) + 1);
    @mkdir(dirname("$target/$relative"), 0777, true);
    copy($file->getPathname(), "$target/$relative");
    $count++;
}
echo "copied $count fixtures to tests/fixtures\n";

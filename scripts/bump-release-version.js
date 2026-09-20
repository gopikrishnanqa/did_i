/**
 * Bump VERSION.json for a store upload, then sync app/build.gradle.kts.
 *
 * Usage:
 *   node scripts/bump-release-version.js           # patch bump (1.0.0 -> 1.0.1)
 *   node scripts/bump-release-version.js --minor
 *   node scripts/bump-release-version.js --major
 *   node scripts/bump-release-version.js --no-bump  # only sync / apply current
 */
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const releasePath = path.join(root, 'VERSION.json');
const gradlePath = path.join(root, 'app', 'build.gradle.kts');

function bumpSemver(version, kind) {
  const parts = String(version)
    .split('.')
    .map((n) => Number.parseInt(n, 10));
  while (parts.length < 3) parts.push(0);
  let [major, minor, patch] = parts;
  if (Number.isNaN(major) || Number.isNaN(minor) || Number.isNaN(patch)) {
    throw new Error(`Invalid version: ${version}`);
  }
  if (kind === 'major') {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (kind === 'minor') {
    minor += 1;
    patch = 0;
  } else {
    patch += 1;
  }
  return `${major}.${minor}.${patch}`;
}

function todayIsoDate() {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function syncGradle(version, versionCode) {
  if (!fs.existsSync(gradlePath)) {
    throw new Error(`Missing ${gradlePath}`);
  }
  const text = fs.readFileSync(gradlePath, 'utf8');
  if (!/versionCode\s*=\s*\d+/.test(text) || !/versionName\s*=\s*"[^"]*"/.test(text)) {
    throw new Error('app/build.gradle.kts: version fields not found to patch');
  }
  const next = text
    .replace(/versionCode\s*=\s*\d+/, `versionCode = ${versionCode}`)
    .replace(/versionName\s*=\s*"[^"]*"/, `versionName = "${version}"`);
  if (next !== text) {
    fs.writeFileSync(gradlePath, next);
  }
}

function main() {
  const args = process.argv.slice(2);
  const noBump = args.includes('--no-bump');
  let kind = 'patch';
  if (args.includes('--major')) kind = 'major';
  if (args.includes('--minor')) kind = 'minor';

  const release = JSON.parse(fs.readFileSync(releasePath, 'utf8'));
  const before = {
    version: release.version,
    androidVersionCode: release.androidVersionCode,
  };

  if (!noBump) {
    release.version = bumpSemver(release.version, kind);
    release.androidVersionCode = Number(release.androidVersionCode || 0) + 1;
    release.releasedAt = todayIsoDate();
    if (!release.notes || /Initial Play Store release/i.test(release.notes)) {
      release.notes = `Play upload ${release.version} (code ${release.androidVersionCode})`;
    }
    fs.writeFileSync(releasePath, `${JSON.stringify(release, null, 2)}\n`);
  }

  syncGradle(release.version, Number(release.androidVersionCode));

  console.log(noBump ? 'Applied current VERSION.json (no bump)' : `Bumped VERSION.json (${kind})`);
  console.log(`  version:            ${before.version} -> ${release.version}`);
  console.log(
    `  androidVersionCode: ${before.androidVersionCode} -> ${release.androidVersionCode}`
  );
  console.log(`  app/build.gradle.kts versionName/versionCode synced`);
  if (release.notes) console.log(`  notes:               ${release.notes}`);
}

main();

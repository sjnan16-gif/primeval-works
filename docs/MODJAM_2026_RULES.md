# CurseForge ModJam 2026 — compliance checklist

This is a working summary, not a substitute for accepting and reading the official rules. Recheck the official pages immediately before publishing because the sponsor reserves the right to change or extend the contest.

Official sources:

- [ModJam 2026 landing page](https://mod.curseforge.com/minecraft/modjam2026/)
- [Official terms and conditions](https://mod.curseforge.com/minecraft/modjam2026/terms-and-conditions/)
- [CurseForge announcement](https://blog.curseforge.com/curseforge-modjam-2026-is-live-bring-history-to-life-in-echoes-of-the-past/)
- [CurseForge moderation policies](https://support.curseforge.com/support/solutions/articles/9000197279-project-and-modpack-moderation-policies)
- [Minecraft usage guidelines](https://www.minecraft.net/usage-guidelines)

## Hard eligibility and submission requirements

- [ ] Entrant has an active CurseForge Mod Author account.
- [ ] Entrant is at least 18 years old or the age of majority where they live, whichever is greater.
- [ ] Participation and prize receipt are legal in the entrant's jurisdiction.
- [ ] Project is new and was not published on CurseForge before July 21, 2026.
- [ ] Project clearly fits **Echoes of the Past**.
- [ ] Java build supports Minecraft 26.1 or higher. Primeval Works targets 26.1.2.
- [ ] Project is submitted in the Java **Mods** category.
- [ ] CurseForge project contains a GitHub link, or judges receive a shared private repository with source visibility.
- [ ] Project is uploaded, approved, live, and available on CurseForge before the cutoff.
- [ ] Official submission form is completed accurately after the project is live.
- [ ] All CurseForge, moderation, Minecraft, and community rules are followed.
- [ ] Project is original and all third-party content/dependencies are licensed and credited appropriately.
- [ ] Project is accurately categorized and not a duplicate of another submitted project.

## Exact deadline

The official end is **September 1, 2026 at 5:00 p.m. GMT+3**. That is 4:00 p.m. in Amsterdam while CEST/UTC+2 is active, but use the official GMT+3 time as the authority.

The project must already be approved and live by the cutoff. Moderation may take several days. Our internal schedule is therefore:

- August 26: release-candidate feature freeze.
- August 27: clean-profile and dedicated-server verification.
- August 28: first CurseForge upload and project submission for moderation.
- August 29–31: moderation fixes, description/media polish, and emergency bugfix file only.
- September 1: verify live project and submitted form well before the official hour.

Do not aim to first upload on August 31 or September 1.

## AI rule

The official 2026 rules explicitly prohibit **AI-generated project avatars or gallery images**.

For Primeval Works:

- The CurseForge avatar/logo must be created by the modeler/artist.
- Gallery images must be real screenshots captured from the running mod or human-authored compositions allowed by the rules.
- Do not use ImageGen output as the icon, header, gallery art, or fake gameplay screenshot.
- Keep original `.bbmodel`, texture, UI, and logo sources as authorship evidence.
- The reviewed rules do not explicitly prohibit AI-assisted programming. This is an inference from the specific written restriction, not a guarantee from CurseForge. All submitted code and content must still be original, lawful, reviewable, safe, and under the entrant's control.

If there is any doubt about a promotional asset, ask CurseForge at `curseforge@overwolf.com` or in the official Discord before submission.

## Originality and intellectual property

- Do not copy Palworld names, creatures, models, textures, sounds, UI, logos, descriptions, or marketing images.
- Do not market the project as an unofficial Palworld conversion. Describe the original mechanic: prehistoric companion-powered base automation.
- Do not import Jurassic Park/Jurassic World visual designs, logos, audio, or named hybrids.
- Use scientifically inspired or wholly original creature interpretations.
- Do not use unlicensed fonts, music, sound libraries, textures, or code.
- GeckoLib is an external MIT-licensed dependency; declare it as required and credit it.
- Retain license notices for reused code/assets where their licenses require it.
- Never submit another person's work as ours, even if it was freely downloadable.

## Moderation and safety

The project page must directly explain what the mod does and what it requires. Avoid empty pages that send users elsewhere for essential information.

The mod/JAR must not contain:

- malware, credential collection, hidden downloads, or executable payloads;
- hateful/discriminatory, sexual, or prohibited extreme content;
- fraudulent links or misleading files;
- copied protected IP;
- mechanisms intended to manipulate downloads, reviews, or votes.

Do not bundle GeckoLib's JAR inside our release unless its distribution and NeoForge packaging requirements explicitly call for it. Declare the CurseForge dependency so the launcher installs the correct version.

## Judging priorities

The official weights are:

| Criterion | Weight | Primeval Works response |
|---|---:|---|
| Originality | 30% | Dinosaurs are persistent workers, logisticians, generators, guardians, and personalities inside one player-designed base system |
| Fun factor | 30% | A fast discovery→hatch→assign→watch→upgrade loop, readable decisions, mutation chase, and recoverable failure |
| Visuals | 30% | Twelve authored models, strong animation silhouettes, consistent ancient-tech UI, visible machines, tint/size individuality, real screenshots |
| Downloads | 10% | Early approved upload, polished page, trailer/GIFs from actual gameplay, compatibility clarity, and rapid crash fixes—never manipulation |

This means a barely functional extra block is worth less than polishing onboarding, one work animation, UI feedback, or screenshots.

## Prize and participation facts

- Fifteen Java Mods grand winners receive $1,000 each.
- Only one Grand Prize can be won per author, even with multiple projects.
- A Community Favorite is selected per category and receives $2,000.
- Community voting is September 8–14, 2026.
- Winners are scheduled for September 17, 2026.
- Prize amounts are pre-tax; the verified Mod Author account holder may need to provide payment and tax information.
- Accurate contact email and CurseForge account data are essential; missed winner communication can forfeit a prize.
- By entering, the entrant grants CurseForge/Overwolf a non-exclusive license to use/display/modify for presentation and distribute the submission for contest/marketing/publicity purposes as described in the rules.
- Do not use bots, multiple identities, fake downloads, fake reviews, automated votes, or coordinated fraudulent engagement.

## Repository and release requirements

- [ ] Git history starts during the contest period/project development and clearly shows original work.
- [ ] README explains the mod, Minecraft version, NeoForge, Java version, and GeckoLib requirement.
- [ ] Source builds from a clean clone using the checked-in Gradle wrapper.
- [ ] GitHub Actions build is green.
- [ ] No secrets, CurseForge tokens, personal paths, or account credentials are committed.
- [ ] Release JAR contains license/credits and correct `neoforge.mods.toml` metadata.
- [ ] CurseForge relationship declares GeckoLib required.
- [ ] Release is tested without IntelliJ/MCreator installed.
- [ ] Release is tested on an actual dedicated server.
- [ ] Project page includes installation, dependencies, core loop, known limitations, and support link.
- [ ] Source repository link is accessible to judges before form submission.

## Project page media plan

All imagery must be artist-authored or captured from the actual game.

Minimum gallery:

1. Hero screenshot: finished living base with several species working.
2. Command Table roster screen with distinct stats/mutations.
3. Hatching sequence from a real Prime Incubator.
4. Logistics route with a dinosaur carrying and depositing an item.
5. Power screen plus visible Water Turbine/Wind Turbine.
6. Guardian defending sleeping workers at night.
7. Species lineup or several clean in-game close-ups.

A short real-game trailer/GIF should show the complete loop in under one minute. Never render features that the submitted build does not contain.

## Final submission sequence

1. Tag a release candidate in Git.
2. Build from a clean checkout with Java 25.
3. Test the exact JAR in a clean CurseForge 26.1.2 NeoForge profile with GeckoLib.
4. Test the exact JAR on a dedicated server.
5. Upload that JAR to the new CurseForge project.
6. Declare version 26.1.2, NeoForge, client and server, and required GeckoLib relation accurately.
7. Submit human-authored avatar and real in-game gallery images.
8. Wait for approval and resolve moderation issues.
9. Confirm the project/file are live and downloadable.
10. Confirm GitHub/shared repo access.
11. Complete the official Typeform with accurate project/contact information.
12. Save confirmation screenshots/email and verify everything well before the cutoff.

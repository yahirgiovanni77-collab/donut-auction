# Donut Auction HUD (Fabric client mod, Minecraft 1.21.11, Java 21)

On-screen auction for streams. Shows seller, item, highest bidder, current bid, minimum bid and a countdown.
Watches chat for payments. Payments below the minimum bid are ignored.

## Use
- **H** opens the settings menu, **G** starts/stops an auction (rebindable in Controls).
- Commands (client-side only):
  `/auctionhud menu | start [seconds] | stop | addtime <+/-s> | preview | reload`
  `/auctionhud seller <name> | item <text> | min <amount> | time <seconds>`
  `/auctionhud set <option> <value>`  (tab-complete lists every option)
  `/auctionhud line <n> <text> | linecolor <n> <#hex> | removeline <n> | resetlines`
- HUD line placeholders: `{seller} {item} {bidder} {bid} {min} {timer}`

## IMPORTANT: payment message
Payments are detected with `paymentRegex` (config/donutauction.json). The default expects
`Steve paid you $1.5K`. If DonutSMP words it differently, copy a real message and edit the regex
(group 1 = name, group 2 = number, group 3 = K/M/B), then `/auctionhud reload`.

## Build
**Easiest:** push this folder to GitHub; the workflow builds the jar (Actions tab -> Artifacts).
See `modrinth/UPLOAD.md` for publishing.
**Locally:** JDK 21 + Gradle 9.2+, run `gradle build`; jar is in `build/libs/` (not the -sources one).
If a dependency fails to download, update the versions in `gradle.properties` from https://fabricmc.net/develop.

# Donut Auction HUD

An on-screen **auction timer** for streamers and traders on DonutSMP. Start an auction and the mod
watches chat for incoming payments, keeps track of the **highest bidder**, and announces the winner
when the timer hits zero.

## Features
- Shows **seller**, **item**, **highest bidder**, **current bid**, **minimum bid** and a **countdown**
- **Minimum bid**: payments below it are ignored and don't count
- Customizable timer, plus optional **anti-snipe** (late bids extend the timer)
- In-game settings menu (default key **H**) and one-key start/stop (default key **G**)
- **Fully customizable HUD**: position, size, colors, background, and every line of text with
  placeholders like `{seller}`, `{item}`, `{bidder}`, `{bid}`, `{min}`, `{timer}`
- Optional public chat announcement of the winner
- Client-side only. It only reads chat and draws a HUD. It never clicks, buys or sells for you.

## Commands
`/auctionhud menu | start [seconds] | stop | addtime <s> | preview | reload`
`/auctionhud seller <name> | item <text> | min <amount> | time <seconds>`
`/auctionhud set <option> <value>` (tab-complete lists every option)
`/auctionhud line <n> <text> | linecolor <n> <#hex> | removeline <n> | resetlines`

## Setup note
Payments are detected with a configurable pattern (`paymentRegex` in `config/donutauction.json`).
If bids don't register, copy a real payment message from your chat and adjust the pattern, then run
`/auctionhud reload`.

## Requirements
Minecraft **1.21.11**, Fabric Loader and **Fabric API**.

*Not affiliated with or endorsed by DonutSMP.*

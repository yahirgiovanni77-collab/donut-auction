# Uploading to Modrinth (step by step)

## 1. Get the jar
Easiest: let GitHub build it.
1. Make a free GitHub account, create a new repository, and upload everything in this folder
   (the `.github` folder must be included; drag-and-drop works on the "uploading an existing file" page).
2. Open the repository's **Actions** tab, click the latest "Build jar" run, wait for the green check.
3. Scroll to **Artifacts**, download `donut-auction-mc1.21.11`, and unzip it. The jar inside
   (donut-auction-1.2.0+mc1.21.11.jar) is the file you upload.
If the run shows a red X, open it and send me the error text.

## 2. Create the project on modrinth.com
Profile menu -> **Create a project**
- Name: Donut Auction HUD
- Summary: On-screen auction timer for DonutSMP with highest bidder, minimum bid and full customization
- Project type: **Mod**
- Client side: **Required**, Server side: **Unsupported**
- License: MIT (put your name in LICENSE first)
- Icon: `modrinth/icon.png`
- Description: paste `modrinth/description.md`

## 3. Upload the version
Versions -> **Create version**
- File: the jar from step 1
- Version number: `1.2.0`
- Game version: **1.21.11**
- Loader: **Fabric**
- Dependencies: add **Fabric API** as *Required*
- Changelog: paste `modrinth/changelog.md`
- Submit the project for review when Modrinth asks. Review usually takes a few days.

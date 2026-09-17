Build Helper for Litematica
==============
Build Helper for Litematica is a client-side Minecraft mod for Fabric.
It is an unofficial addon for [Litematica](https://github.com/maruohon/litematica) that helps you while building a schematic by hand.
It is not affiliated with or endorsed by masa or the Litematica project.

Requires [Litematica](https://github.com/sakura-ryoko/litematica) and [MaLiLib](https://github.com/sakura-ryoko/malilib).

For compiled builds (= downloads), see https://github.com/LwozR/build-helper-for-litematica/releases

Features
========
* Highlights every spot in the schematic where the block in your main hand still needs to be placed
* An info panel with the block icon and name, how many are left, how many you have (shulker boxes included) and how many are missing
* Next block suggestion: when you are not holding a needed block, the panel suggests the most needed block and where it is in your inventory
* Progress bars for the current render layer and for the whole schematic
* A message and a sound when the current render layer is complete
* Auto next layer: moves to the next render layer when the current one is complete
* Block order: automatic, or a manual order that you set separately for every layer
* Container highlight: needed items in chests, barrels and shulker boxes are outlined green, and items that can be crafted, cut or smelted into needed blocks are outlined yellow
* Container tooltip: shows what an item is needed for and roughly how many are needed
* Follows the Litematica render layers: when a layer mode is active, the counts and suggestions only use that layer
* Follows the game language (English, Türkçe, Deutsch, Español, Français, Português, Русский, Українська, Polski, Italiano, 简体中文, 繁體中文, 日本語, 한국어)

Usage
=====
* Load a schematic in Litematica and hold one of its blocks
* Open the settings with `M + B` or through Mod Menu
* Open the block order of the current layer with `M + O`
* Every feature can be toggled and the panel position, scale and colors can be changed

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/

License
=======
LGPLv3, the same as Litematica and MaLiLib.

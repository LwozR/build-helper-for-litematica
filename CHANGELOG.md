Changelog
=========

1.1.0
-----
* Auto next layer: in single layer mode the render layer moves on when the current layer is complete, skipping empty and finished layers
* Block order: automatic or manual, with a separate order for every layer (M + O)
* Container highlight: needed items are outlined green, items that can be crafted, cut or smelted into needed blocks are outlined yellow
* Container tooltip: shows the item id and what the item is needed for in the current layer or schematic

1.0.2
-----
* Ignored materials now count as done, so ignoring blocks never lowers the progress
* Blocks that only differ in a neighbor based shape (wall, fence and pane connections, stair corners, waterlogged) count as correct
* Blocks without an item, such as fluids, are no longer counted

1.0.1
-----
* Blocks ignored in the Litematica material list are left out of counts, suggestions, highlights and progress
* Builds for Minecraft 26.2 and 26.1

1.0.0
-----
* Held block highlight
* Info panel with remaining, inventory and missing counts
* Layer aware next block suggestion
* Layer and total progress bars
* Layer complete message and sound
* 14 languages

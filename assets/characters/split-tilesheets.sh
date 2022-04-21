#!/bin/bash

# Based on http://samclane.github.io/cutting-a-kenney-sprite-sheet-with-imagemagick/

# set -x

mkdir -p tiles
rm tiles/*.png

# Use -define png:color-type=6 to prevent greyscale sprites from using the LinearGray space.
# The end result is that the transparent pixels get misrepresented by the libgdx texture packer as a solid black background.
# See https://github.com/libgdx/libgdx/issues/4814 and https://github.com/eowise/gradle-packer-plugin/pull/12/files

convert body.png -colorspace RGB -define png:color-type=6 -crop 2x4-1-1@ +repage +adjoin tiles/body_%d_.png
convert legs.png -colorspace RGB -define png:color-type=6 -crop 2x10-1-1@ +repage +adjoin tiles/legs_%d_.png
convert torso.png -colorspace RGB -define png:color-type=6 -crop 12x10-1-1@ +repage +adjoin tiles/torso_%d_.png
convert hair.png -colorspace RGB -define png:color-type=6 -crop 20x3-1-1@ +repage +adjoin tiles/hair_%d_.png
convert beard.png -colorspace RGB -define png:color-type=6 -crop 20x1-1-1@ +repage +adjoin tiles/beard_%d_.png

# Lint the output by checking for known issues...

# If we mis-sized the input tilesheet by a pixel or two, thens ome of the images will not end up 16x16 pixels as desired.
# Usually just a matter of adding a pixel or two to the input image, and moving the image around a few pixels to match.
identify tiles/*.png | grep -v " 16x16 "

# Greyscale images can be misinterpreted as greyscale output PNGs by imagemagic. See above for description
# of why this is problematic with libgdx texture packer turning the transparent pixels to solid black.
identify tiles/*.png | grep "LinearGray"

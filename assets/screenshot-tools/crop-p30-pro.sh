# The screenshots are typically taken on a physical P30 Pro.
# This script will remove the strip along the top and the navigation bar along the bottom.
# 
# Should be run from the screenshots directory.
mogrify -crop 2160x1080+70+0 *.png

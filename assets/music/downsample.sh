#!/bin/bash
#
# Creates a lower bitrate version of each MP3 in the ./original/ folder.
#

set -x

readonly DIR=$(dirname "$0")
readonly SRC_DIR="$DIR/SpaceJacked OST/original"
readonly DOWNSAMPLED_DIR="$DIR/SpaceJacked OST/downsampled"

mkdir -p "$DOWNSAMPLED_DIR"

find "$SRC_DIR" -iname "*.mp3" -print0 | while read -d $'\0' MP3_PATH
do
    MP3_NAME=$(basename "$MP3_PATH")
    DEST_FILE="$DOWNSAMPLED_DIR/$MP3_NAME"

    if [[ ! -f "$DEST_FILE" || $* == *--force* ]]
    then
        echo "MP3_PATH: '$MP3_PATH'"
        lame --mp3input -b 56 "$MP3_PATH" "$DEST_FILE"
    else
        echo "Skipping $MP3_NAME, already exists (use --force to overwrite)"
    fi
done

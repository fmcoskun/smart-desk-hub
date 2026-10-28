#!/bin/bash
# Generates the demo camera clips in app/src/main/res/raw from ffmpeg's built-in synthetic
# sources (no camera footage, no third-party content). Run once; the outputs are checked in.
#   FFMPEG=/path/to/ffmpeg tools/make_clips.sh
set -euo pipefail
FFMPEG=${FFMPEG:-ffmpeg}
OUT="$(dirname "$0")/../app/src/main/res/raw"
mkdir -p "$OUT"
# -g 15: a keyframe every second (15 fps), so a seek never decodes more than 1 s of video
ENC=(-c:v libx264 -profile:v baseline -pix_fmt yuv420p -r 15 -g 15 -keyint_min 15 -crf 30 -an -movflags +faststart -y)

# clip <name> <seconds> <colour 1> <colour 2> <box colour> <box period s> <box y>
# A slow gradient "room", film grain, a vignette, and a box walking across = a person/motion.
clip() {
  local name=$1 secs=$2 c1=$3 c2=$4 box=$5 period=$6 y=$7
  "$FFMPEG" -hide_banner -loglevel error \
    -f lavfi -i "gradients=s=640x360:r=15:c0=$c1:c1=$c2:speed=0.004:d=$secs" \
    -vf "drawbox=x='(iw+120)*mod(t,$period)/$period-120':y=$y:w=70:h=150:color=$box@0.85:t=fill,\
drawbox=x=0:y=ih-60:w=iw:h=60:color=black@0.25:t=fill,noise=alls=10:allf=t,vignette=PI/5" \
    -t "$secs" "${ENC[@]}" "$OUT/$name.mp4"
}

# Live loops: 8 s, the walker crosses exactly once so the loop restarts cleanly
clip live_desk     8 0x3b4a5c 0x8a9bb0 0x2c3036 8 170
clip live_door     8 0x5c4a3b 0xb0a08a 0x1d2733 8 160
clip live_meeting  8 0x3b5c4f 0x8ab0a0 0x33261d 8 175
clip live_hallway  8 0x4a3b5c 0xa08ab0 0x26331d 8 165

# Recordings: two per camera, in that camera's colours (a Door recording looks like the Door
# feed). Exact lengths: the Recordings list shows these durations.
clip rec_desk_15     15 0x3b4a5c 0x8a9bb0 0x2c3036 5  170
clip rec_desk_45     45 0x3b4a5c 0x8a9bb0 0x2c3036 9  170
clip rec_door_30     30 0x5c4a3b 0xb0a08a 0x1d2733 10 160
clip rec_door_60     60 0x5c4a3b 0xb0a08a 0x1d2733 12 160
clip rec_meeting_15  15 0x3b5c4f 0x8ab0a0 0x33261d 5  175
clip rec_meeting_60  60 0x3b5c4f 0x8ab0a0 0x33261d 12 175
clip rec_hallway_30  30 0x4a3b5c 0xa08ab0 0x26331d 10 165
clip rec_hallway_45  45 0x4a3b5c 0xa08ab0 0x26331d 9  165

const mongoose = require("mongoose")

const playlistSchema = mongoose.Schema(
  {
    id: {
      type: Number,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
    description: {
      type: String,
      default: "",
    },
    coverArtUrl: {
      type: String,
      default: "",
    },
    createdBy: {
      type: String,
      required: true,
      ref: "User",
    },
    isPublic: {
      type: Boolean,
      default: true,
    },
    songs: [
      {
        type: String,
        ref: "Music",
      },
    ],
  },
  {
    timestamps: true,
  },
)

module.exports = mongoose.model("Playlist", playlistSchema)

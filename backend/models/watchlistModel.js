const mongoose = require("mongoose")

const watchlistSchema = mongoose.Schema(
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
    createdBy: {
      type: String,
      required: true,
      ref: "User",
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

module.exports = mongoose.model("Watchlist", watchlistSchema)

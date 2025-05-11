const express = require("express")
const router = express.Router()
const {
  createWatchlist,
  getUserWatchlists,
  getWatchlistById,
  updateWatchlist,
  deleteWatchlist,
  addSongToWatchlist,
  removeSongFromWatchlist,
  checkSongInWatchlists,
} = require("../controllers/watchlistController")
const { protect } = require("../middleware/authMiddleware")

// All routes are protected
router.post("/", protect, createWatchlist)
router.get("/", protect, getUserWatchlists)
router.get("/:id", protect, getWatchlistById)
router.put("/:id", protect, updateWatchlist)
router.delete("/:id", protect, deleteWatchlist)
router.post("/:id/songs", protect, addSongToWatchlist)
router.delete("/:id/songs/:musicId", protect, removeSongFromWatchlist)
router.get("/check/:musicId", protect, checkSongInWatchlists)

module.exports = router

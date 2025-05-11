const express = require("express")
const router = express.Router()
const {
    createPlaylist,
    getUserPlaylists,
    getPlaylistById,
    updatePlaylist,
    deletePlaylist,
    addSongToPlaylist,
    removeSongFromPlaylist,
} = require("../controllers/playlistController")
const { protect } = require("../middleware/authMiddleware")

router.post("/", protect, createPlaylist)
router.get("/", protect, getUserPlaylists)
router.get("/:id", protect, getPlaylistById)
router.put("/:id", protect, updatePlaylist)
router.delete("/:id", protect, deletePlaylist)
router.post("/:id/songs", protect, addSongToPlaylist)
router.delete("/:id/songs/:musicId", protect, removeSongFromPlaylist)

module.exports = router
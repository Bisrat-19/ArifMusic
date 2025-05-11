const asyncHandler = require("express-async-handler")
const Playlist = require("../models/playlistModel")


const createPlaylist = asyncHandler(async(req, res) => {
    const { id, name, description, coverArtUrl, isPublic } = req.body

    if (!id || !name) {
        res.status(400)
        throw new Error("Please provide playlist ID and name")
    }

    const playlistExists = await Playlist.findOne({ id })

    if (playlistExists) {
        res.status(400)
        throw new Error("Playlist with this ID already exists")
    }

    const playlist = await Playlist.create({
        id,
        name,
        description: description || "",
        coverArtUrl: coverArtUrl || "",
        createdBy: req.user.id,
        isPublic: isPublic !== undefined ? isPublic : true,
        songs: [],
    })

    if (playlist) {
        res.status(201).json(playlist)
    } else {
        res.status(400)
        throw new Error("Invalid playlist data")
    }
})


const getUserPlaylists = asyncHandler(async(req, res) => {
    const playlists = await Playlist.find({ createdBy: req.user.id })
    res.json(playlists)
})


const getPlaylistById = asyncHandler(async(req, res) => {
    const playlist = await Playlist.findOne({ id: req.params.id })

    if (!playlist) {
        res.status(404)
        throw new Error("Playlist not found")
    }

    if (!playlist.isPublic && playlist.createdBy !== req.user.id && req.user.userType !== "ADMIN") {
        res.status(403)
        throw new Error("Not authorized to view this playlist")
    }

    res.json(playlist)
})


const updatePlaylist = asyncHandler(async(req, res) => {
    const playlist = await Playlist.findOne({ id: req.params.id })

    if (!playlist) {
        res.status(404)
        throw new Error("Playlist not found")
    }

    if (playlist.createdBy !== req.user.id && req.user.userType !== "ADMIN") {
        res.status(403)
        throw new Error("Not authorized to update this playlist")
    }

    playlist.name = req.body.name || playlist.name
    playlist.description = req.body.description !== undefined ? req.body.description : playlist.description
    playlist.coverArtUrl = req.body.coverArtUrl || playlist.coverArtUrl
    playlist.isPublic = req.body.isPublic !== undefined ? req.body.isPublic : playlist.isPublic

    const updatedPlaylist = await playlist.save()
    res.json(updatedPlaylist)
})

const deletePlaylist = asyncHandler(async(req, res) => {
    const playlist = await Playlist.findOne({ id: req.params.id })

    if (!playlist) {
        res.status(404)
        throw new Error("Playlist not found")
    }

    if (playlist.createdBy !== req.user.id && req.user.userType !== "ADMIN") {
        res.status(403)
        throw new Error("Not authorized to delete this playlist")
    }

    await playlist.remove()
    res.json({ message: "Playlist removed" })
})

const addSongToPlaylist = asyncHandler(async(req, res) => {
    const { musicId } = req.body

    if (!musicId) {
        res.status(400)
        throw new Error("Please provide a music ID")
    }

    const playlist = await Playlist.findOne({ id: req.params.id })

    if (!playlist) {
        res.status(404)
        throw new Error("Playlist not found")
    }

    if (playlist.createdBy !== req.user.id && req.user.userType !== "ADMIN") {
        res.status(403)
        throw new Error("Not authorized to update this playlist")
    }

    if (playlist.songs.includes(musicId)) {
        res.status(400)
        throw new Error("Song already in playlist")
    }

    playlist.songs.push(musicId)
    const updatedPlaylist = await playlist.save()

    res.json(updatedPlaylist)
})

const removeSongFromPlaylist = asyncHandler(async(req, res) => {
    const playlist = await Playlist.findOne({ id: req.params.id })

    if (!playlist) {
        res.status(404)
        throw new Error("Playlist not found")
    }

    if (playlist.createdBy !== req.user.id && req.user.userType !== "ADMIN") {
        res.status(403)
        throw new Error("Not authorized to update this playlist")
    }

    const songIndex = playlist.songs.indexOf(req.params.musicId)
    if (songIndex === -1) {
        res.status(400)
        throw new Error("Song not in playlist")
    }

    playlist.songs.splice(songIndex, 1)
    const updatedPlaylist = await playlist.save()

    res.json(updatedPlaylist)
})

module.exports = {
    createPlaylist,
    getUserPlaylists,
    getPlaylistById,
    updatePlaylist,
    deletePlaylist,
    addSongToPlaylist,
    removeSongFromPlaylist,
}
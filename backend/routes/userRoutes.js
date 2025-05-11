const express = require("express")
const router = express.Router()
const {
    registerUser,
    loginUser,
    getUserProfile,
    updateUserProfile,
    getUserById,
    getUserByEmail,
    checkUserExists,
    deleteUser,
    updateUserType,
    submitVerificationRequest,
    resetPassword,
    approveArtist,
    suspendUser,
    updateArtistApprovalStatus,
} = require("../controllers/userController")
const { protect, admin } = require("../middleware/authMiddleware")

router.post("/register", registerUser)
router.post("/login", loginUser)
router.get("/exists/:email", checkUserExists)
router.post("/reset-password", resetPassword)

router.get("/profile", protect, getUserProfile)
router.put("/profile", protect, updateUserProfile)
router.get("/:id", protect, getUserById)
router.get("/email/:email", protect, getUserByEmail)
router.delete("/:id", protect, deleteUser)

router.put("/:id/type", protect, admin, updateUserType)
router.put("/:id/approve", protect, admin, approveArtist)
router.put("/:id/suspend", protect, admin, suspendUser)


module.exports = router
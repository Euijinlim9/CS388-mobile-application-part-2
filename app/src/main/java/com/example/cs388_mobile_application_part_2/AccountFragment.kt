package com.example.cs388_mobile_application_part_2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import androidx.core.content.edit
import androidx.core.net.toUri
@SuppressLint("SetTextI18n")
class AccountFragment : Fragment() {

    private val PREFS = "user_prefs"
    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_LOGGED_IN = "logged_in"
    private val KEY_PHOTO_URI = "photo_uri"

    private lateinit var layoutLoggedOut: LinearLayout
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var tvWelcome: TextView
    private lateinit var imgProfile: ImageView

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            savePhotoUri(uri.toString())
            loadProfilePhoto(uri.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        layoutLoggedOut = view.findViewById(R.id.layoutLoggedOut)
        layoutLoggedIn = view.findViewById(R.id.layoutLoggedIn)
        tvWelcome = view.findViewById(R.id.tvWelcome)
        imgProfile = view.findViewById(R.id.imgProfilePhoto)

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_LOGGED_IN, false)) {
            showLoggedIn(prefs.getString(KEY_USERNAME, "") ?: "")
            loadProfilePhoto(prefs.getString(KEY_PHOTO_URI, null))
        }
        showLoggedOut()
        setupLogin(view, prefs)
        setupRegister(view, prefs)

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit { putBoolean(KEY_LOGGED_IN, false) }
            showLoggedOut()
        }

        view.findViewById<Button>(R.id.btnChangePhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pickImage.launch(intent)
        }
    }

    private fun setupLogin(view: View, prefs: android.content.SharedPreferences) {
        val etUser = view.findViewById<EditText>(R.id.etLoginUsername)
        val etPass = view.findViewById<EditText>(R.id.etLoginPassword)
        val tvError = view.findViewById<TextView>(R.id.tvLoginError)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            val savedUser = prefs.getString(KEY_USERNAME, null)
            val savedPass = prefs.getString(KEY_PASSWORD, null)

            if (user == savedUser && pass == savedPass) {
                prefs.edit { putBoolean(KEY_LOGGED_IN, true) }
                showLoggedIn(user)
                loadProfilePhoto(prefs.getString(KEY_PHOTO_URI, null))
                tvError.visibility = View.GONE
                setupSettings(view, prefs)
            } else {
                tvError.text = "Invalid username or password"
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRegister(view: View, prefs: android.content.SharedPreferences) {
        val etUser = view.findViewById<EditText>(R.id.etRegUsername)
        val etPass = view.findViewById<EditText>(R.id.etRegPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etRegConfirmPassword)
        val tvError = view.findViewById<TextView>(R.id.tvRegError)

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            val confirm = etConfirm.text.toString()

            when {
                user.isEmpty() -> { tvError.text = "Username cannot be empty"; tvError.visibility = View.VISIBLE }
                pass.length < 6 -> { tvError.text = "Password must be at least 6 characters"; tvError.visibility = View.VISIBLE }
                pass != confirm -> { tvError.text = "Passwords do not match"; tvError.visibility = View.VISIBLE }
                prefs.contains(KEY_USERNAME) -> { tvError.text = "An account already exists. Please login."; tvError.visibility = View.VISIBLE }
                else -> {
                    prefs.edit {
                        putString(KEY_USERNAME, user)
                            .putString(KEY_PASSWORD, pass)
                            .putBoolean(KEY_LOGGED_IN, true)
                    }
                    showLoggedIn(user)
                    tvError.visibility = View.GONE
                    setupSettings(view, prefs)
                }
            }
        }
    }

    private fun setupSettings(view: View, prefs: android.content.SharedPreferences){
        val etUser = view.findViewById<EditText>(R.id.etChangeUsername)
        val etPass = view.findViewById<EditText>(R.id.etChangePassword)
        val etOldPass = view.findViewById<EditText>(R.id.etOldPassword)
        val tvError = view.findViewById<TextView>(R.id.tvChangeError)

        view.findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            val oldPass = etOldPass.text.toString()
            val savedPass = prefs.getString(KEY_PASSWORD, null)
            if (oldPass == savedPass){
                if(user.isNotEmpty()){
                    prefs.edit { putString(KEY_USERNAME, user) }
                    showLoggedIn(user)
                }
                if(pass.isNotEmpty()){
                    prefs.edit { putString(KEY_PASSWORD, pass) }
                }
                tvError.visibility = View.GONE
            }else{
                tvError.text = "Invalid password"
                tvError.visibility = View.VISIBLE
            }
            etUser.text.clear()
            etPass.text.clear()
            etOldPass.text.clear()
        }
    }
    private fun showLoggedIn(username: String) {
        layoutLoggedOut.visibility = View.GONE
        layoutLoggedIn.visibility = View.VISIBLE
        tvWelcome.text = "Welcome, $username!"
    }

    private fun showLoggedOut() {
        layoutLoggedOut.visibility = View.VISIBLE
        layoutLoggedIn.visibility = View.GONE
    }

    private fun savePhotoUri(uri: String) {
        requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_PHOTO_URI, uri) }
    }

    private fun loadProfilePhoto(uriString: String?) {
        if (!uriString.isNullOrEmpty()) {
            Glide.with(this).load(uriString.toUri()).circleCrop().into(imgProfile)
        }
    }
}

package app.carpecoin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import app.carpecoin.coin.databinding.ActivityMainBinding
import androidx.navigation.Navigation.findNavController
import app.carpecoin.coin.R
import app.carpecoin.firebase.FirestoreHelper
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        FirestoreHelper.initialize(this)
    }

    override fun onSupportNavigateUp() = findNavController(navHostFragment.view!!).navigateUp()
}
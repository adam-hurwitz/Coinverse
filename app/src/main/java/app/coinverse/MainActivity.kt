package app.coinverse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation.findNavController
import app.coinverse.databinding.ActivityMainBinding
import app.coinverse.firebase.FirestoreHelper
import app.coinverse.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        FirestoreHelper.initialize(this)
        val utils = Utils
        utils.resources = resources
    }

    override fun onSupportNavigateUp() = findNavController(navHostFragment.view!!).navigateUp()
}
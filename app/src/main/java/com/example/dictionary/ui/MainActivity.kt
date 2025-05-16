package com.example.dictionary.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dictionary.R
import com.example.dictionary.databinding.ActivityMainBinding
import com.example.dictionary.util.DatabaseHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Đảm bảo sử dụng NavHostFragment để lấy NavController
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Thiết lập BottomNavigationView với NavController
            binding.bottomNavigation.setupWithNavController(navController)

            // Thêm animation cho bottom navigation
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                // Tạo hiệu ứng ripple
                val view = binding.bottomNavigation.findViewById<View>(item.itemId)
                view?.let {
                    it.isPressed = true
                    it.postDelayed({ it.isPressed = false }, 100)
                }

                // Chuyển đến destination
                navController.navigate(item.itemId)
                true
            }

            // Thêm xử lý để giữ trạng thái của fragment
            binding.bottomNavigation.setOnItemReselectedListener { /* Không làm gì khi chọn lại tab hiện tại */ }

            // Thiết lập theme cho menu
            this.theme.applyStyle(R.style.PopupMenuStyle, true)

        } catch (e: Exception) {
            // Xử lý lỗi khi khởi tạo UI
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng: ${e.message}", Toast.LENGTH_LONG).show()

            // Thử reset database nếu có lỗi
            try {
                databaseHelper.resetDatabase()
                recreate() // Khởi động lại activity
            } catch (e2: Exception) {
                Toast.makeText(this, "Không thể khôi phục ứng dụng. Vui lòng cài đặt lại.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

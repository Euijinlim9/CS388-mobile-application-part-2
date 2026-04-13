package com.example.cs388_mobile_application_part_2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = MainPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "News"
                1 -> "Game Finder"
                2 -> "Tools"
                3 -> "Account"
                else -> ""
            }
        }.attach()
    }

    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 4
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> NewsFragment()
            1 -> GameFinderFragment()
            2 -> ToolsFragment()
            3 -> AccountFragment()
            else -> NewsFragment()
        }
    }
}

package com.enazamusic.smsapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.enazamusic.smsapp.model.ViewPagerElement

class ViewPagerAdapter(fragmentManager: FragmentManager, private val elementsList: MutableList<ViewPagerElement>):
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return elementsList.size
    }

    override fun getItem(position: Int): Fragment {
        return elementsList[position].fragment
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return elementsList[position].title
    }
}
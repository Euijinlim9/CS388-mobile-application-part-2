package com.example.cs388_mobile_application_part_2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class UpcomingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_upcoming, container, false)

        val db = (requireActivity().application as PetApplication).db
        val recyclerView = view.findViewById<RecyclerView>(R.id.upcoming_list)
        val upcomingAdapter = UpcomingAdapter()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = upcomingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.eventDao().getUpcomingEvents().collect { events ->
                upcomingAdapter.submitList(events)
            }
        }

        return view
    }
}
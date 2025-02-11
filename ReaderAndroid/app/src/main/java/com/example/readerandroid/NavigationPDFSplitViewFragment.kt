package com.kontinua.readerandroid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NavigationPDFSplitViewFragment : Fragment() {

    private var workbooks: List<Workbook>? = null
    private var selectedWorkbookID: String? = null
    private lateinit var workbookListView: ListView
    private lateinit var pdfViewFragment: PDFViewFragment

    companion object {
        private const val BASE_URL = "http://localhost:8000/"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_navigation_split_view, container, false)
        workbookListView = view.findViewById(R.id.workbookListView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfViewFragment = PDFViewFragment()
        childFragmentManager.commit {
            replace(R.id.pdfViewContainer, pdfViewFragment)
        }

        fetchWorkbooks()

        workbookListView.setOnItemClickListener { _, _, position, _ ->
            val selectedWorkbook = workbooks?.get(position)
            selectedWorkbook?.let {
                selectedWorkbookID = it.id
                pdfViewFragment.setFileName(it.pdfName) // Update the PDFViewFragment with the selected PDF
            }
        }
    }

    private fun fetchWorkbooks() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.getWorkbooks()

        call.enqueue(object : Callback<List<Workbook>> {
            override fun onResponse(call: Call<List<Workbook>>, response: Response<List<Workbook>>) {
                if (response.isSuccessful) {
                    workbooks = response.body()
                    val workbookTitles = workbooks?.map { it.id } ?: emptyList()
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, workbookTitles)
                    workbookListView.adapter = adapter
                } else {
                    Log.e("NavigationPDFSplitView", "Failed to fetch workbooks: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<Workbook>>, t: Throwable) {
                Log.e("NavigationPDFSplitView", "Network error: ${t.message}")
            }
        })
    }
}
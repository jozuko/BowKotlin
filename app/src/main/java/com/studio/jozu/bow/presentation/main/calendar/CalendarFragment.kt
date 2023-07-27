package com.studio.jozu.bow.presentation.main.calendar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.studio.jozu.bow.databinding.CalendarFragmentBinding
import com.studio.jozu.bow.di.BowComponent
import com.studio.jozu.bow.domain.extension.DisposableListEx.dispose
import com.studio.jozu.bow.infrastructure.event.EventBusManager
import com.studio.jozu.bow.infrastructure.event.model.OnAddDogEvent
import com.studio.jozu.bow.presentation.main.MainActivityFragmentListener
import com.studio.jozu.bow.usecase.CalendarCase
import com.studio.jozu.bow.usecase.DogListCase
import com.studio.jozu.bow.usecase.EventCase
import com.studio.jozu.bow.usecase.TaskListCase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

class CalendarFragment : Fragment() {
    companion object {
        /**
         * CalendarFragmentの生成を行う
         */
        fun newInstance(): CalendarFragment {
            return CalendarFragment().apply {
                arguments = Bundle().apply {

                }
            }
        }
    }

    @Inject
    lateinit var calendarCase: CalendarCase

    @Inject
    lateinit var dogListCase: DogListCase

    @Inject
    lateinit var eventCase: EventCase

    @Inject
    lateinit var taskListCase: TaskListCase

    @Inject
    lateinit var eventBusManager: EventBusManager

    private var listener: MainActivityFragmentListener? = null

    private val disposableList = mutableListOf<Disposable>()

    private var binding: CalendarFragmentBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivityFragmentListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BowComponent.instance.inject(this)
    }

    override fun onResume() {
        super.onResume()
        eventBusManager.register(this)
    }

    override fun onPause() {
        super.onPause()
        eventBusManager.unregister(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = CalendarFragmentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        disposableList.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpCalendarPager()
    }

    private fun setUpCalendarPager() {
        val binding = binding ?: return
        val disposable = dogListCase.getDogListFromLocal()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dogList ->
                    binding.calendarPager.adapter = CalendarPagerAdapter(
                        monthList = calendarCase.allMonthList,
                        dogList = dogList,
                        eventCase = eventCase,
                        dogListCase = dogListCase,
                        taskListCase = taskListCase,
                        binding.calendarPager,
                    ).apply {
                        onPrev = {
                            binding.calendarPager.setCurrentItem(binding.calendarPager.currentItem - 1, true)
                        }
                        onNext = {
                            binding.calendarPager.setCurrentItem(binding.calendarPager.currentItem + 1, true)
                        }
                    }
                    var pagerIndex = calendarCase.todayMonthIndex
                    if (pagerIndex < 0) {
                        pagerIndex = 0
                    }
                    binding.calendarPager.setCurrentItem(pagerIndex, false)
                },
                {
                    Timber.e("CalendarFragment#setUpCalendarPager: ${it.localizedMessage}")
                }
            )
        disposableList.add(disposable)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAddDogEvent(event: OnAddDogEvent) {
        val binding = binding ?: return
        binding.calendarPager.adapter?.notifyDataSetChanged()
    }
}
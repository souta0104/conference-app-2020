package io.github.droidkaigi.confsched2020

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.android.support.DaggerAppCompatActivity
import io.github.droidkaigi.confsched2020.data.repository.SessionRepository
import io.github.droidkaigi.confsched2020.databinding.ActivityMainBinding
import io.github.droidkaigi.confsched2020.di.PageScope
import io.github.droidkaigi.confsched2020.ext.assistedActivityViewModels
import io.github.droidkaigi.confsched2020.ext.stringRes
import io.github.droidkaigi.confsched2020.session.ui.MainSessionsFragment
import io.github.droidkaigi.confsched2020.session.ui.MainSessionsFragmentModule
import io.github.droidkaigi.confsched2020.session.ui.SearchSessionsFragment
import io.github.droidkaigi.confsched2020.session.ui.SearchSessionsFragmentModule
import io.github.droidkaigi.confsched2020.session.ui.SessionDetailFragment
import io.github.droidkaigi.confsched2020.session.ui.SessionDetailFragmentModule
import io.github.droidkaigi.confsched2020.session.ui.di.SessionAssistedInjectModule
import io.github.droidkaigi.confsched2020.system.ui.viewmodel.SystemViewModel
import timber.log.Timber
import timber.log.debug
import javax.inject.Inject
import javax.inject.Provider

class MainActivity : DaggerAppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(
            this,
            R.layout.activity_main
        )
    }
    @Inject
    lateinit var systemViewModelProvider: Provider<SystemViewModel>
    private val systemViewModel: SystemViewModel by assistedActivityViewModels {
        systemViewModelProvider.get()
    }
    @Inject
    lateinit var sessionRepository: SessionRepository
    val navController: NavController by lazy {
        Navigation.findNavController(this, R.id.root_nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.main),
            binding.drawerLayout
        ) {
            onBackPressed()
            true
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.navView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            handleNavigation(item.itemId)
        });

        systemViewModel.errorLiveData.observe(this) { appError ->
            Timber.debug(appError) { "AppError occured" }
            Snackbar
                .make(
                    findViewById(R.id.root_nav_host_fragment),
                    appError.stringRes(),
                    Snackbar.LENGTH_LONG
                )
                .show()
        }
    }

    private fun handleNavigation(@IdRes itemId: Int): Boolean {
        return try {
            // ignore if current destination is selected
            if (navController.currentDestination?.id == itemId) return false
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.main, false)
            val options = builder.build()
            navController.navigate(itemId, null, options)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

@Module
abstract class MainActivityModule {
    @Binds
    abstract fun providesActivity(mainActivity: MainActivity): FragmentActivity

    @PageScope
    @ContributesAndroidInjector(
        modules = [MainSessionsFragmentModule::class, SessionAssistedInjectModule::class]
    )
    abstract fun contributeSessionsFragment(): MainSessionsFragment

    @PageScope
    @ContributesAndroidInjector(
        modules = [SessionDetailFragmentModule::class, SessionAssistedInjectModule::class]
    )
    abstract fun contributeSessionDetailFragment(): SessionDetailFragment

    @PageScope
    @ContributesAndroidInjector(
        modules = [SearchSessionsFragmentModule::class]
    )
    abstract fun contributeSearchSessionsFragment(): SearchSessionsFragment

    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideNavController(mainActivity: MainActivity): NavController {
            return mainActivity.navController
        }
    }

    @Module
    abstract class MainActivityBuilder {
        @ContributesAndroidInjector(modules = [MainActivityModule::class])
        abstract fun contributeMainActivity(): MainActivity
    }
}
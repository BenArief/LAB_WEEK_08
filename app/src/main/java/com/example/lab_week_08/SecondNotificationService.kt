package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper

class SecondNotificationService : Service() {
    //In order to make the required notification, a service is required
    //to do the job for us in the foreground process

    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder
    //Create a system handler which controls what thread the process is being
//    executed on
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()

        val channelId = createNotificationChannel()

        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    //A pending Intent is the Intent used to be executed
    //when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id
            val channelId = "002"
            //Create the channel name
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )
            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            //Binds the channel into the NotificationManager
            //NotificationManager will trigger the notification later on
            service.createNotificationChannel(channel)

            //Return the channel id
            channelId
        } else { "" }
    //Build the notification with all of its contents and configurations

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId:
    String) =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Third worker process is done")
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent,
            flags, startId)

        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        //Posts the notification task to the handler,
        //which will be executed on a different thread
        serviceHandler.post {
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)
            //Here we're notifying the MainActivity that the service process is
//            done
            //by returning the channel ID through LiveData
            notifyCompletion(Id)
            //Stops the foreground service, which closes the notification
            //but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)
            //Stop and destroy the service
            stopSelf()
        }
        return returnValue
    }
    //A function to update the notification to display a count down from 10 to
//    0
    private fun countDownFromTenToZero(notificationBuilder:
                                       NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager

        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
//the Main Thread is identified by calling the "getMainLooper()" method
//This function is called after the count down has completed
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }


    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        //this is a LiveData which is a data holder that automatically
        //updates the UI based on what is observed
        //It'll return the channel ID into the LiveData after
        //the countdown has reached 0, giving a sign that
        //the service process is done
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}

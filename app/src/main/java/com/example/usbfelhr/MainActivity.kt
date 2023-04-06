package com.example.usbfelhr

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {

    private val someLiveData: MutableLiveData<Boolean>? = null
    private lateinit var mList: ArrayList<ReadModelData>
    private lateinit var adapter: ReadAdapter
    lateinit var m_usbManager: UsbManager
    lateinit var viewModel: MainActivityViewModel
    var m_device: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null

    val ACTION_USB_PERMISSION = "permission"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerview = findViewById<RecyclerView>(R.id.rv)
        recyclerview.layoutManager = LinearLayoutManager(this)
        mList = ArrayList()
        adapter = ReadAdapter(mList)
        recyclerview.adapter = adapter


        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager


        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(broadcastReceiver, filter)

        val on = findViewById<Button>(R.id.btn_on)
        val off = findViewById<Button>(R.id.btn_off)
        val disconnect = findViewById<Button>(R.id.btn_disconnect)
        val connect = findViewById<Button>(R.id.btn_connect)

        viewModel.seconds().observe(this, Observer {
            mList.add(ReadModelData(it))
            adapter.notifyDataSetChanged()
            someLiveData?.postValue(null)
            recyclerview.smoothScrollToPosition(mList.size - 1)

        })

        on.setOnClickListener { readData() }
        off.setOnClickListener { writeData("0") }
        disconnect.setOnClickListener { disconnect() }
        connect.setOnClickListener { startUsbConnecting() }

    }

    private fun startUsbConnecting() {
        val clmain = findViewById<ConstraintLayout>(R.id.main)
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var keep = true
            usbDevices.forEach { entry ->
                m_device = entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                Log.i("serial", "vendorId: " + deviceVendorId)
                if (deviceVendorId == 9025) {
                    val intent: PendingIntent =
                        PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                    m_usbManager.requestPermission(m_device, intent)
                    keep = false
                    Log.i("serial", "connection successful")
                    val snackBar =
                        Snackbar.make(clmain, "USB Connection Successful", Snackbar.LENGTH_LONG)
                    snackBar.show()
                } else {
                    m_connection = null
                    m_device = null
                    Log.i("serial", "unable to connect")
                    Toast.makeText(this, "USB unable to connect", Toast.LENGTH_LONG).show()
                }
                if (!keep) {
                    return
                }
            }
        } else {
            Log.i("serial", "no usb device connected")
            Toast.makeText(this, "no usb device connected", Toast.LENGTH_LONG).show()
        }
    }

    private fun writeData(input: String) {
        m_serial?.write(input.toByteArray())
        Log.i("serial", "sending data: " + input.toByteArray())
    }

    private fun readData() {
        val mCallback = UsbSerialInterface.UsbReadCallback { bytes ->
            //  val readDataString = bytes.toString(Charsets.UTF_8)
              val readDataString = String(bytes)
            //  val readDataString = String(bytes, Charsets.UTF_8)
            //  val readDataString = bytes.toString(StandardCharsets.UTF_8)
            //  val readDataString = bytes.decodeToString()
          //  val readDataString = bytes.toString()
            Log.i("Serial", "Data received: $readDataString")
            viewModel._seconds.postValue(readDataString)
        }
        m_serial?.read(mCallback)
    }

    private fun disconnect() {
        m_serial?.close()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                val granted: Boolean =
                    intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    m_connection = m_usbManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device, m_connection)
                    if (m_serial != null) {
                        if (m_serial!!.open()) {
                            m_serial!!.setBaudRate(9600)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                        } else {
                            Log.i("Serial", "port not open")
                        }
                    } else {
                        Log.i("Serial", "port is null")
                    }
                } else {
                    Log.i("Serial", "permission not granted")
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                startUsbConnecting()
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                disconnect()
            }
        }
    }

}
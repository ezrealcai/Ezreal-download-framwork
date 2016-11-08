package com.ef.efstudents_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.ef.efstudents_android.download.DownloadInfo;
import com.ef.efstudents_android.download.DownloadManager;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button button;
    private Button button2;
	private Button crashButton;
    private DownloadManager downloadManager;
    private DownloadManager.DownloadObserver downloadObserver;
    private ProgressBar progressBar2;
    private ProgressBar progressBar3;
    private long start1;
    private long start2;
    private long start3;
    private long end1;
    private long end2;
    private long end3;

	private String s;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar3 = (ProgressBar) findViewById(R.id.progressBar3);
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
		crashButton = (Button) findViewById(R.id.crashbutton);

		final String downloadUrl = "http://dzs.qisuu.com/txt/%E4%BF%AE%E7%9C%9F%E7%8B%82%E5%B0%91%E5%9C%A8%E6%A0%A1%E5%9B%AD.txt";
        final String downloadUrl2 = "http://dzs.qisuu.com/txt/%E7%99%BE%E4%B8%87%E4%BB%99%E5%AE%97.txt";
        final String downloadUrl3 = "http://dzs.qisuu.com/txt/%E4%BB%99%E8%B7%AF%E4%BA%89%E9%94%8B.txt";

        downloadManager = DownloadManager.getInstance().init(this);
        downloadObserver = new DownloadManager.DownloadObserver() {
            @Override
            public void onDownloadStateChange(DownloadInfo downloadInfo) {
                if (downloadInfo.getState() == DownloadManager.STATE_FINISH) {
                    String url = downloadInfo.getDownloadUrl();
                    if (url.equals(downloadUrl)) {
                        end1 = System.currentTimeMillis();
                        System.out.println(url + ": " + ((end1 - start1) / 1000));
                    } else if (url.equals(downloadUrl2)) {
                        end2 = System.currentTimeMillis();
                        System.out.println(url + ": " + ((end2 - start2) / 1000));
                    } else if (url.equals(downloadUrl3)) {
                        end3 = System.currentTimeMillis();
                        System.out.println(url + ": " + ((end3 - start3) / 1000));
                    }
                }
            }

            @Override
            public void onDownloadProgressChange(DownloadInfo downloadInfo) {
                int progress = (int) (downloadInfo.getCurrentLength() * 1.f / downloadInfo
                        .getSize() * 100);
                String url = downloadInfo.getDownloadUrl();
                System.out.println(url + ": " + progress);
                if (url.equals(downloadUrl)) {
                    progressBar.setProgress(progress);
                } else if (url.equals(downloadUrl2)) {
                    progressBar2.setProgress(progress);
                } else if (url.equals(downloadUrl3)) {
                    progressBar3.setProgress(progress);
                }
            }
        };
        downloadManager.registerDownloadObserver(downloadObserver);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.download(downloadUrl);
                start1 = System.currentTimeMillis();
                downloadManager.download(downloadUrl2);
                start2 = System.currentTimeMillis();
                downloadManager.download(downloadUrl3);
                start3 = System.currentTimeMillis();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.pause(downloadUrl);
                downloadManager.pause(downloadUrl2);
                downloadManager.pause(downloadUrl3);
            }
        });

		crashButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				System.out.println(s.equals("s"));
			}
		});

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.unregisterDownloadObserver(downloadObserver);
    }

}

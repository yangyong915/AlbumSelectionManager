package com.luck.picture.lib.tools;

import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 裁剪视频工具类
 */
public class TrimVideoUtils {
	
	/** 单例对象 */
	private static TrimVideoUtils instance = null;
	
	/** 私有的默认构造函数 */
	private TrimVideoUtils() {
	}
	
	/** 获取单例实体对象 */
	public static TrimVideoUtils getInstance() {
		if (instance == null) {
			instance = new TrimVideoUtils();
		}
		return instance;
	}
	
	// =============== 回调事件  ===============
	/** 裁剪回调接口 */
	private TrimFileCallBack trimCallBack;
	
	/**
	 * 设置裁剪回调
	 * @param trimCallBack
	 */
	public void setTrimCallBack(TrimFileCallBack trimCallBack) {
		this.trimCallBack = trimCallBack;
	}

	/** 裁剪文件回调接口 */
	public interface TrimFileCallBack {
		/**
		 * 裁剪回调
		 * @param isNew 是否新剪辑
		 * @param startS 开始时间(秒)
		 * @param endS 结束时间(秒)
		 * @param vTotal 视频长度
		 * @param file 需要裁剪的文件路径
		 * @param trimFile 裁剪后保存的文件路径
		 */
        void trimCallback(boolean isNew, int startS, int endS,
                          int vTotal, File file, File trimFile);
		
		/**
		 * 裁剪失败回调
		 * @param eType 错误类型
		 */
        void trimError(int eType);
	}
	
	// =============== 对外公开方法  ===============
	/** 是否暂停裁剪 */
	private boolean isStopTrim = false;
	// -- 常量 --
	/** 裁剪保存的文件地址 */
	public static final String TRIM_SAVE_PATH = "trimSavePath";
	/** 裁剪选择 */
	public static final int TRIM_SWITCH = -8;
	/** 停止裁剪 */
	public static final int TRIM_STOP = -9;
	/** 文件不存在 */
	public static final int FILE_NOT_EXISTS = -10;
	/** 裁剪失败 */
	public static final int TRIM_FAIL = -11;
	/** 裁剪成功 */
	public static final int TRIM_SUCCESS = -12;
	
	/** 暂停裁剪 */
	public void stopTrim(){
		isStopTrim = true;
	}
	
	/**
	 * 裁剪回调
	 * @param isNew 是否新剪辑(true = 新剪辑，false = 修改原剪辑-覆盖)
	 * @param startS 开始时间(秒)
	 * @param endS 结束时间(秒)
	 * @param file 需要裁剪的文件路径
	 * @param trimFile 裁剪后保存的文件路径
	 */
	public void startTrim(boolean isNew, int startS, int endS, File file, File trimFile){
		// 默认非暂停裁剪
		isStopTrim = false;
		// 需要裁剪的视频必须存在
		if(file != null && file.exists()){
			try {
				// 获取文件地址
				String path = file.getAbsolutePath();
				// 生成Movie对象(解析视频信息)
				Movie movie = MovieCreator.build(path);
				// 获取视频轨道信息(视频流、声道)
				List<Track> tracks = movie.getTracks();
				// 设置新的轨道信息
				movie.setTracks(new LinkedList<Track>());
				// 关键则 - 时间是否准确的
				boolean timeCorrected = false;
				// 开始裁剪的时间
				double startTime = startS;
				// 结束裁剪的时间
				double endTime = endS;
				// 获取轨道条数
				// int totalSize = tracks.size();
				// 遍历轨道,获取视频轨道的帧数
				for (Track track : tracks) {
					if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
						if (timeCorrected) {
							throw new RuntimeException( "The startTime has already been corrected by another track with SyncSample. Not Supported.");
						}
						// true = 获取与裁剪时间最大值关键帧时间
						// false = 获取与裁剪时间最接近的时间
						// --
						// true, false表示短截取, false,true表示长截取
						startTime = correctTimeToSyncSample(track, startTime, false);
						endTime = correctTimeToSyncSample(track, endTime, true);
						timeCorrected = true;
					}
				}
				// 遍历计算裁剪时间(视频、音频) - 重新计算裁剪时间
				for (Track track : tracks) {
					long currentSample = 0;
					double currentTime = 0;
					long startSample = -1;
					long endSample = -1;
					// 获取编码过的数据
					List<TimeToSampleBox.Entry> listEntrys = track.getDecodingTimeEntries();
					// 遍历,计算关键帧差
					for (int i = 0, c = listEntrys.size(); i < c; i++) {
						TimeToSampleBox.Entry entry = listEntrys.get(i);
						for (int j = 0; j < entry.getCount(); j++) {
							if (currentTime <= startTime) {
								startSample = currentSample;
							}
							if (currentTime <= endTime) {
								endSample = currentSample;
							} else {
								break;
							}
							currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
							currentSample++;
						}
					}
					movie.addTrack(new CroppedTrack(track, startSample, endSample));
				}
				if(isStopTrim){
					if(this.trimCallBack != null){
						this.trimCallBack.trimError(TRIM_STOP);
					}
					return;
				}
				// 进行处理裁剪视频
				Container container = new DefaultMp4Builder().build(movie);
				// 判断文件是否存在，不存在则创建
				if (!trimFile.exists()) {
					trimFile.createNewFile();
				}
				// 写入流
				FileOutputStream fos = new FileOutputStream(trimFile);
				FileChannel fc = fos.getChannel();
				container.writeContainer(fc);
				fc.close();
				fos.close();
				// --
				if (isStopTrim) {
					if (trimFile.exists()) {
						trimFile.delete();
					}
					if(this.trimCallBack != null){
						this.trimCallBack.trimError(TRIM_STOP);
					}
					return;
				}
				// 裁剪成功
				if(this.trimCallBack != null){
					// 裁剪视频的总时间
					int vTotal = endS - startS;
					// 触发回调
					this.trimCallBack.trimCallback(isNew, startS, endS, vTotal, file, trimFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
				// --
				if(this.trimCallBack != null){
					this.trimCallBack.trimError(TRIM_FAIL);
				}
				// --
				try {
					if (trimFile.exists()) {
						trimFile.delete();
					}
				} catch (Exception e2) {
				}
			}
		} else {
			if(this.trimCallBack != null){
				this.trimCallBack.trimError(FILE_NOT_EXISTS);
			}
		}
	}
	
	/**
	 * 计算关键帧时间
	 * @param file 文件路径
	 * @param dTime 计算失败默认返回时间
	 */
	public double reckonFrameTime(File file, double dTime){
		// 文件必须存在
		if(file != null && file.exists()){
			try {
				// 获取文件地址
				String path = file.getAbsolutePath();
				// 生成Movie对象(解析视频信息)
				Movie movie = MovieCreator.build(path);
				// 获取视频轨道信息(视频流、声道)
				List<Track> tracks = movie.getTracks();
				// 设置新的轨道信息
				movie.setTracks(new LinkedList<Track>());
				// 关键则 - 时间是否准确的
				boolean timeCorrected = false;
				// 获取轨道条数
				// int totalSize = tracks.size();
				// 遍历轨道,获取视频轨道的帧数
				for (Track track : tracks) {
					if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
						if (timeCorrected) {
							throw new RuntimeException( "The startTime has already been corrected by another track with SyncSample. Not Supported.");
						}
						double oFrame = correctTimeToSyncSample(track);
						timeCorrected = true;
						return oFrame;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dTime;
	}
	
	// --
	
	/**
	 * 改正正确的关键帧时间
	 * @param track
	 * @return
	 */
	private double correctTimeToSyncSample(Track track) {
		// 时间偏差量计算
		double[] timeOfSyncSamples = new double[]{-1, -1, -1, -1, -1};
		// timeOfSyncSamples = new double[track.getSyncSamples().length];
		// 偏移量数组长度
		int tLength = timeOfSyncSamples.length;
		// 当前偏差量
		long currentSample = 0;
		// 当前时间
		double currentTime = 0;
		// 是否返回
		boolean isBreak = false;
		// 获取编码过的数据
		List<TimeToSampleBox.Entry> listEntrys = track.getDecodingTimeEntries();
		// 遍历,计算关键帧差
		for (int i = 0, c = listEntrys.size(); i < c; i++) {
			if(isBreak){
				break;
			}
			TimeToSampleBox.Entry entry = listEntrys.get(i);
			for (int j = 0; j < entry.getCount(); j++) {
				// 获取偏移量索引
				int tofPos = Arrays.binarySearch(track.getSyncSamples(), currentSample + 1);
				if (tofPos >= 0) {
					// 防止大于数组长度
					if(tofPos >= tLength){
						isBreak = true;
						break;
					}
					timeOfSyncSamples[tofPos] = currentTime;
				}
				currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
				currentSample++;
			}
		}
		// 获取偏移量
		double offset = -1d;
		// -- 计算关键帧 --
		for(int i = 1, c = timeOfSyncSamples.length; i < c; i++){
			double above = timeOfSyncSamples[i-1];
			double curr = timeOfSyncSamples[i];
			if(curr != -1d && above != -1){
				double tOffset = curr - above;
				if(offset == -1d){ // 初始化第一个计算出来的偏移量
					offset = tOffset;
				} else {
					if(offset < tOffset){ // 如果最新的偏移量比上次的大,则重新保存
						offset = tOffset;
					}
					// 这段代码，直接返回比上次偏移量大的
					//if(tOffset > offset){
					//	return tOffset;
					//} else {
					//	return offset;
					//}
				}
			}
		}
		if(offset == -1d){
			return timeOfSyncSamples[1];
		}
		return offset;
	}
	
	/**
	 * 改正正确的关键帧时间(关联)
	 * @param track
	 * @param cutHere 裁剪的时间
	 * @param next 是否获取最大值
	 * @return
	 */
	private double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
		// 偏移量数组长度
		int tLength = track.getSyncSamples().length;
		// 时间偏差量计算
		double[] timeOfSyncSamples = new double[tLength];
		// 当前偏差量
		long currentSample = 0;
		// 当前时间
		double currentTime = 0;
		// 获取编码过的数据
		List<TimeToSampleBox.Entry> listEntrys = track.getDecodingTimeEntries();
		// 遍历,计算关键帧差
		for (int i = 0, c = listEntrys.size(); i < c; i++) {
			TimeToSampleBox.Entry entry = listEntrys.get(i);
			for (int j = 0; j < entry.getCount(); j++) {
				// 获取偏移量索引
				int tofPos = Arrays.binarySearch(track.getSyncSamples(), currentSample + 1);
				if (tofPos >= 0) {
					timeOfSyncSamples[tofPos] = currentTime;
				}
				currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
				currentSample++;
			}
		}
		// 获取裁剪位置，之间的关键帧数据
		double previous = 0;
		// 遍历全部关键帧时间
		for (double timeOfSyncSample : timeOfSyncSamples) {
			if (timeOfSyncSample > cutHere) {
				if (next) {
					return timeOfSyncSample;
				} else {
					return previous;
				}
			}
			previous = timeOfSyncSample;
		}
		return timeOfSyncSamples[timeOfSyncSamples.length - 1];
	}
}

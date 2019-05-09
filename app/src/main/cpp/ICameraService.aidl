/**
 * @file: ICameraService.aidl
 * @version: 1
 * @description:
 *      definition for MeiG Camera Service interfaces
 */

package meig;

interface ICameraService
{
    // ******************* cam_id **********************
    const int CAM_ID_0 = 0;
    const int CAM_ID_1 = 1;
    const int CAM_ID_2 = 2;
    const int CAM_ID_3 = 3;
    const int CAM_ID_4 = 4;
    const int CAM_ID_TOTAL = 5;

    /**
     * @function: open
     * @description:
     *      interface to open camera, to setup all environment for specified camera, and register callbcack method
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @paremeter:  cb_binder,  IBinder as callback method
     * @return:     return 0 if success, return otherwise for error code
     */
    int open( int cam_id, IBinder cb_binder );

    /**
     * @function: close
     * @description:
     *      interface to close camera, to finalize all environment for specified camera
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @return:     return 0 if success, return otherwise for error code
     */
    int close( int cam_id );

    /**
     * @function: start_stream
     * @function: stop_stream
     * @description:
     *      interface to start camera stream flow, start capture, and data stream will send through callback(setup in open)
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @paremeter:  stream_id,  specify which stream to operate, value shoud be one of: STREAM_ID_*
     * @return:     return 0 if success, return otherwise for error code
     */
    int start_stream( int cam_id, int stream_id, FileDescriptor shared_fd, int shared_size );
    int stop_stream( int cam_id, int stream_id );
    const int STREAM_ID_VIDEO_RAW = 0;
    const int STREAM_ID_VIDEO_ENCODED = 1;
    const int STREAM_ID_VIDEO_LOCAL_RECORD = 2;
    const int STREAM_ID_VIDEO_PRE_RECORD = 3;
    const int STREAM_ID_AUDIO_PCM = 4;
    const int STREAM_ID_TOTAL = 5;

	/**
     * @function: snapshot
     * @description:
     *      interface to take snapshot on camera stream flow, default formart jpg, path of file will notify through callback(setup in open)
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @return:     return 0 if success, return otherwise for error code
     */
    int snapshot( int cam_id );

    /**
     * @function: get_status
     * @description:
     *      interface to get current status of specify camera
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @return:     a string to of current status description
     */
    Utf8InCppString get_status( int cam_id );

    /**
     * @function: get_last_error
     * @description:
     *      interface to get last error of specify camera
     * @paremeter:  cam_id,     specify which camera to operate, value shoud be one of: CAM_ID_*
     * @return:     a string to of last error
     */
    Utf8InCppString get_last_error( int cam_id );


    // ******************* configs *********************
    int set_config_audio_sample_rate( int cam_id, int config_audio_sample_rate );
    int get_config_audio_sample_rate( int cam_id );
    const int CONFIG_AUDIO_SAMPLE_RATE_8000 = 0;
    const int CONFIG_AUDIO_SAMPLE_RATE_16000 = 1;
    const int CONFIG_AUDIO_SAMPLE_RATE_44100 = 2;
    const int CONFIG_AUDIO_SAMPLE_RATE_48000 = 4;
 
    int set_config_audio_chanel_num( int cam_id, int config_audio_chanel_num );
    int get_config_audio_chanel_num( int cam_id );
    const int CONFIG_AUDIO_CHANEL_NUM_NONO= 1;
    const int CONFIG_AUDIO_CHANEL_NUM_STEREO = 2;
    
    int set_config_audio_sample_type( int cam_id, int config_audio_sample_type );
    int get_config_audio_sample_type( int cam_id );
    const int CONFIG_AUDIO_SAMPLE_TYPE_S16 = 0;
    const int CONFIG_AUDIO_SAMPLE_TYPE_U16 = 1;
    const int CONFIG_AUDIO_SAMPLE_TYPE_S32 = 2;
    const int CONFIG_AUDIO_SAMPLE_TYPE_U32 = 3;
  
    int set_config_video_resolution( int cam_id, int config_video_resolution );
    int get_config_video_resolution( int cam_id );
    const int CONFIG_VIDEO_RESOLUTION_1920_1080 = 0;
    const int CONFIG_VIDEO_RESOLUTION_1280_720 = 1;
   
    int set_config_video_frame_rate( int cam_id, int config_video_frame_rate );
    int get_config_video_frame_rate( int cam_id );
    const int CONFIG_VIDEO_FRAME_RATE_25 = 25;
    const int CONFIG_VIDEO_FRAME_RATE_30 = 30;

    int set_config_video_prerecord_len( int cam_id, int config_video_prerecord_len );
    int get_config_video_prerecord_len( int cam_id );
    const int CONFIG_VIDEO_PRERECORD_LEN_NONE = 0;
    const int CONFIG_VIDEO_PRERECORD_LEN_10s = 1;
    const int CONFIG_VIDEO_PRERECORD_LEN_30s = 2;
    const int CONFIG_VIDEO_PRERECORD_LEN_60s = 3;

    int set_config_video_color_mode( int cam_id, int config_video_color_mode );
    int get_config_video_color_mode( int cam_id );
    const int CONFIG_VIDEO_COLOR_MODE_NORMAL = 0;
    const int CONFIG_VIDEO_COLOR_MODE_BLACK_WHITE = 1;

    int set_config_video_codec_format( int cam_id, int config_video_codec_format );
    int get_config_video_codec_format( int cam_id );
    const int CONFIG_VIDEO_CODEC_FORMAT_H264 = 0;
    const int CONFIG_VIDEO_CODEC_FORMAT_H265 = 1;

    int set_config_video_codec_bit_rate( int cam_id, int config_video_codec_bit_rate );
    int get_config_video_codec_bit_rate( int cam_id );
    const int CONFIG_VIDEO_CODEC_BIT_RATE_192K    =   192000;
    const int CONFIG_VIDEO_CODEC_BIT_RATE_512K    =   512000;
    const int CONFIG_VIDEO_CODEC_BIT_RATE_1M      =  1000000;
    const int CONFIG_VIDEO_CODEC_BIT_RATE_2M      =  2000000;
    const int CONFIG_VIDEO_CODEC_BIT_RATE_8M      =  8000000;
    const int CONFIG_VIDEO_CODEC_BIT_RATE_20M     = 20000000;
}

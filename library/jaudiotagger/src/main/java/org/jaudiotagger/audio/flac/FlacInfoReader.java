/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.flac;

import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.metadatablock.BlockType;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataStreamInfo;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentReader;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Read info from Flac file
 */
public class FlacInfoReader
{
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private VorbisCommentReader vorbisCommentReader = new VorbisCommentReader();

    public FlacAudioHeader read(Path path) throws CannotReadException, IOException
    {
        logger.config(path + ":start");
        try(FileChannel fc = FileChannel.open(path))
        {
            FlacStreamReader flacStream = new FlacStreamReader(fc, path.toString() + " ");
            flacStream.findStream();

            MetadataBlockDataStreamInfo mbdsi = null;
            boolean isLastBlock = false;
            String mqaEncoder = null;
            String originalSampleRate = null;

            //Search for StreamInfo Block, but even after we found it we still have to continue through all
            //the metadata blocks so that we can find the start of the audio frames which we need to calculate
            //the bitrate
            while (isLastBlock==false)
            {
                MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(fc);
                logger.info(path.toString() + " "  + mbh.toString());
                if (mbh.getBlockType() == BlockType.STREAMINFO)
                {
                    //See #253:MetadataBlockDataStreamInfo exception when bytes length is 0
                    if(mbh.getDataLength()==0)
                    {
                        throw new CannotReadException(path + ":FLAC StreamInfo has zeo data length");
                    }

                    mbdsi = new MetadataBlockDataStreamInfo(mbh, fc);
                    if (!mbdsi.isValid())
                    {
                        throw new CannotReadException(path + ":FLAC StreamInfo not valid");
                    }
              /*  }else if(mbh.getBlockType() == BlockType.VORBIS_COMMENT && mqaEncoder ==null) {
                    ByteBuffer commentHeaderRawPacket = ByteBuffer.allocate(mbh.getDataLength());
                    fc.read(commentHeaderRawPacket);
                    VorbisCommentTag tag = vorbisCommentReader.read(commentHeaderRawPacket.array(), false, path);
                    if(tag.hasField(FlacAudioHeader.MQA_ENCODER)) {
                        mqaEncoder = tag.getFirst(FlacAudioHeader.MQA_ENCODER);
                    }
                    if(tag.hasField(FlacAudioHeader.MQA_ORIGINAL_SAMPLE_RATE)) {
                        originalSampleRate = tag.getFirst(FlacAudioHeader.MQA_ORIGINAL_SAMPLE_RATE);
                    }else if(tag.hasField(FlacAudioHeader.MQA_SAMPLE_RATE)) {
                        originalSampleRate = tag.getFirst(FlacAudioHeader.MQA_SAMPLE_RATE);
                    } */
                }
                else {
                    fc.position(fc.position() + mbh.getDataLength());
                }
                isLastBlock = mbh.isLastBlock();
            }

            //Audio continues from this point to end of file (normally - TODO might need to allow for an ID3v1 tag at file end ?)
            long streamStart = fc.position();

            if (mbdsi == null)
            {
                throw new CannotReadException(path + ":Unable to find Flac StreamInfo");
            }

           // if((mqaEncoder==null || mqaEncoder.length()==0) && originalSampleRate!=null) {
           //     mqaEncoder = originalSampleRate;
           // }

            FlacAudioHeader info = new FlacAudioHeader();
            info.setNoOfSamples(mbdsi.getNoOfSamples());
            info.setPreciseLength(mbdsi.getPreciseLength());
            info.setChannelNumber(mbdsi.getNoOfChannels());
            info.setSamplingRate(mbdsi.getSamplingRate());
            info.setBitsPerSample(mbdsi.getBitsPerSample());
            info.setEncodingType(mbdsi.getEncodingType());
            info.setFormat(SupportedFileFormat.FLAC.getDisplayName());
            info.setLossless(true);
            info.setMd5(mbdsi.getMD5Signature());
            info.setAudioDataLength(fc.size() - streamStart);
            info.setAudioDataStartPosition(streamStart);
            info.setAudioDataEndPosition(fc.size());
            info.setBitRate(computeBitrate(info.getAudioDataLength(), mbdsi.getPreciseLength()));
            /*info.setMqaEncoder(mqaEncoder);
            info.setMqaInfo(originalSampleRate);
            if(mqaEncoder == null) {
                info.setMqaEncoder(originalSampleRate);
            } */

            return info;
        }
    }

    private int computeBitrate(long size, float length )
    {
        return (int) ((size / Utils.KILOBYTE_MULTIPLIER) * Utils.BITS_IN_BYTE_MULTIPLIER / length);
    }

    /**
     * Count the number of metadatablocks, useful for debugging
     *
     * @param f
     * @return
     * @throws CannotReadException
     * @throws IOException
     */
    public int countMetaBlocks(File f) throws CannotReadException, IOException
    {
        try(FileChannel fc = FileChannel.open(f.toPath()))
        {
            FlacStreamReader flacStream = new FlacStreamReader(fc, f.toPath().toString() + " ");
            flacStream.findStream();

            boolean isLastBlock = false;

            int count = 0;
            while (!isLastBlock)
            {
                MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(fc);
                logger.config(f + ":Found block:" + mbh.getBlockType());
                fc.position(fc.position() + mbh.getDataLength());
                isLastBlock = mbh.isLastBlock();
                count++;
            }
            return count;
        }
    }
}

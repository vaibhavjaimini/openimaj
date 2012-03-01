/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.hadoop.tools.twitter.token.mode;

import org.kohsuke.args4j.CmdLineOptionsProvider;
import org.openimaj.hadoop.tools.twitter.HadoopTwitterTokenToolOptions;

/**
 * A twitter tweet token counting mode
 * 
 * @author ss
 *
 */
public enum TwitterTokenModeOption implements CmdLineOptionsProvider{
	/**
	 * Calculates DF-IDF for each term as described by: "Event Detection in Twitter" by J. Weng et. al. 
	 */
	DFIDF {
		@Override
		public void perform(HadoopTwitterTokenToolOptions opts) {
//			Multi stage DF-IDF process:
//				Calculate DF for a word in a time period (t) = number of tweets with word in time period (t) / number of tweets in time period (t)
//				Calculate IDF = number of tweets up to final time period (T) / number of tweets with word up to time period (T)
//
//				function(timePeriodLength)
//				So a word in a tweet can happen in the time period between t - 1 and t.
//				First task:
//					map input:
//						tweetstatus # json twitter status with JSONPath to words
//					map output:
//						<word: <timePeriod,T>> # each word with the time period (quantised) and final time period (potentially start time of job)
//					reduce input:
//						<word: [<timePeriod,T>,...,<timePeriod,T>]> # all time period, final time pairs
//					reduce output:
//						<word: <<timePeriod,count>,<timePeriod,count>,...,<T,count>> # time period sums and final time sum for word
//
//				Second task:
//					map input:
//						<word: <<timePeriod,count>,<timePeriod,count>,...,<T,count>> # time period sums and final time sum for word
//					map output:
//						<timePeriod: <<word@timeperiod, count>,<word@T, count>>,... > # time period and count of word@t and word@T
//					reduce input:
//						<timePeriod: [(<word, count>,<word@T, count>),(<word, count>,<word@T, count>),...]> # all word counts and word@T counts at this timeperiod
//					reduce output:
//						# for a this timeperiod t, DFIDF for each word calculated using:
//						# DF = count of word (single read) / sum of all word counts (all words read)
//						# IDF = sum of all word@T counts (all read) / word@T count (single read)
//						<word: <timePeriod, DFIDF>,...> #
		}
	};

	@Override
	public Object getOptions() {
		return this;
	}

	/**
	 * @param opts
	 */
	public abstract void perform(HadoopTwitterTokenToolOptions opts);
	
	
}
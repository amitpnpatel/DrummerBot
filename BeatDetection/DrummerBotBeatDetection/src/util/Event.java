/*
	Copyright (C) 2001, 2006 by Simon Dixon

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along
	with this program (the file gpl.txt); if not, download it from
	http://www.gnu.org/licenses/gpl.txt or write to the
	Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package util;

public class Event implements Comparable, Cloneable, java.io.Serializable {

	public double keyDown, keyUp, pedalUp, scoreBeat, scoreDuration, salience;
	public int midiPitch, midiVelocity, flags, midiCommand, midiChannel,
				midiTrack;
	//public String label;

	public Event(double onset, double offset, double eOffset, int pitch,
				 int velocity, double beat, double duration, int eventFlags,
				 int command, int channel, int track) {
		this(onset, offset, eOffset, pitch, velocity, beat,duration,eventFlags);
		midiCommand = command;
		midiChannel = channel;
		midiTrack = track;
	} // constructor

	public Event(double onset, double offset, double eOffset, int pitch,
				 int velocity, double beat, double duration, int eventFlags) {
		keyDown = onset;
		keyUp = offset;
		pedalUp = eOffset;
		midiPitch = pitch;
		midiVelocity = velocity;
		scoreBeat = beat;
		scoreDuration = duration;
		flags = eventFlags;
		midiCommand = javax.sound.midi.ShortMessage.NOTE_ON;
		midiChannel = 1;
		midiTrack = 0;
		salience = 0;
	} // constructor

	public Event clone() {
		return new Event(keyDown, keyUp, pedalUp, midiPitch, midiVelocity,
					scoreBeat, scoreDuration, flags, midiCommand, midiChannel,
					midiTrack);
	} // clone()

	// Interface Comparable
	public int compareTo(Object o) {
		Event e = (Event) o;
		return (int)Math.signum(keyDown - e.keyDown);
	} // compareTo()

	public String toString() {
		return "n=" + midiPitch + " v=" + midiVelocity + " t=" + keyDown +
				" to " + keyUp + " (" + pedalUp + ")";
	} // toString()


} // class Event

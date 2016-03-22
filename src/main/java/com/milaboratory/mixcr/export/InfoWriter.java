/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.mixcr.export;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public final class InfoWriter<T> extends AbstractInfoWriter<T> {

	public InfoWriter(OutputStream outputStream) {
		super(outputStream);
	}

	public InfoWriter(String s) throws FileNotFoundException {
		super(s);
	}

    private void ensureInitialized() {
        if (!initialized) {
            try {
                for (int i = 0; i < fieldExtractors.size(); ++i) {
                    outputStream.write(fieldExtractors.get(i).getHeader().getBytes());
                    if (i == fieldExtractors.size() - 1)
                        break;
                    outputStream.write('\t');
                }
                outputStream.write('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }
    }

    @Override
    public void put(T t) {
        ensureInitialized();
        try {
            for (int i = 0; i < fieldExtractors.size(); ++i) {
                outputStream.write(fieldExtractors.get(i).extractValue(t).getBytes());
                if (i == fieldExtractors.size() - 1)
                    break;
                outputStream.write('\t');
            }
            outputStream.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

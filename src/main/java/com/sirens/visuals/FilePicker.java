package com.sirens.visuals;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Native "open file" dialog. Uses LWJGL's tinyfd through reflection so a missing class can never break the build. */
public final class FilePicker {
    private FilePicker() { }

    public static void pick(String title, Consumer<Path> cb) {
        Thread t = new Thread(() -> {
            String r = null;
            try {
                Class<?> c = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                for (Method m : c.getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("tinyfd_openFileDialog") && p.length == 5 && p[0] == CharSequence.class) {
                        r = (String) m.invoke(null, title, System.getProperty("user.home") + java.io.File.separator, null, "Images", Boolean.FALSE);
                        break;
                    }
                }
            } catch (Throwable ignored) {
                try {
                    java.awt.FileDialog fd = new java.awt.FileDialog((java.awt.Frame) null, title, java.awt.FileDialog.LOAD);
                    fd.setVisible(true);
                    if (fd.getFile() != null) r = fd.getDirectory() + fd.getFile();
                } catch (Throwable t2) { Hooks.error("file dialog", t2); }
            }
            if (r != null && !r.isBlank()) {
                Path path = Path.of(r.split("\\|")[0]);
                Minecraft.getInstance().execute(() -> cb.accept(path));
            }
        }, "sirens-filepicker");
        t.setDaemon(true);
        t.start();
    }
}
